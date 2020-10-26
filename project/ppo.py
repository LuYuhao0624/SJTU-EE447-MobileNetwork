import gym
import numpy as np
import tensorflow as tf
import matplotlib.pyplot as plt
import tensorflow_probability as tfp
from tensorflow.keras.layers import Dense
from tensorflow.keras.optimizers import Adam, RMSprop

tf.keras.backend.set_floatx("float64")
tfd = tfp.distributions
MAX_EPISODE = 5000
MAX_STEP = 200
BATCH_SIZE = 32

class Actor(tf.keras.Model):

    def __init__(self, action_dim, k, eps=1e-8):
        super().__init__()
        self.action_dim = action_dim
        self.k = k
        self.fc1 = Dense(128, "relu")
        self.fc2 = Dense(action_dim, "softmax")
        self.eps = eps

    def call(self, inputs, training=None, mask=None):
        output = self.fc1(inputs)
        output = self.fc2(output)
        return output

    def evaluate_probs(self, memory_states, memory_actions):
        # memory_actions should be list of length N
        memory_states_arr = np.array(memory_states)
        actions_probs = self.call(memory_states_arr) # (N, a_dim)
        dist = tfd.Categorical(probs=actions_probs)
        actions = tf.transpose(tf.convert_to_tensor(memory_actions)) # (k, N)
        actions_prob = tf.transpose(dist.prob(actions)) # (N, k)
        actions_prob = tf.reduce_prod(actions_prob, axis=-1) / \
                    (self.normalizer(actions_probs, self.k) + self.eps) # (N,)
        actions_prob = tf.clip_by_value(actions_prob, 0, 1)
        return actions_prob

    def f(self, p, m):
        return tf.math.reduce_sum(tf.math.pow(p, m), axis=-1) # (N,)

    def normalizer(self, p, k):
        cross = [1]
        fs = [0]
        for i in range(1, k + 1):
            fs.append(self.f(p, i))
        for n in range(1, k + 1):
            res = 0
            sign = 1
            for i in range(1, n + 1):
                res += sign * cross[-i] * fs[i]
                sign = -sign
            cross.append(res / n)
        return cross[-1]

    # def normalizer(self, p, k):
    #     if k == 1:
    #         return self.f(p, 1)
    #     elif k == 2:
    #         return (self.f(p, 1) ** 2 - self.f(p, 2)) / 2
    #     elif k == 3:
    #         return (self.f(p, 1) ** 3 - 3 * self.f(p, 1) * self.f(p, 2)
    #                 + 2 * self.f(p, 3)) / 6
    #     else:
    #         raise NotImplementedError("Actions containing over 4 edges are "
    #                                   "not supported now.")


class Critic(tf.keras.Model):

    def __init__(self):
        super().__init__()
        self.fc1 = Dense(128, "relu")
        self.fc2 = Dense(1)

    def call(self, inputs, training=None, mask=None):
        output = self.fc1(inputs)
        output = self.fc2(output)
        return output


class Memory:

    def __init__(self):
        self.states = []
        self.actions = []
        self.rewards = []
        self.probs = []
        self.dones = []
        self.advs = []

    def memorize(self, state, action, reward, prob, done, adv):
        self.states.append(state)
        self.actions.append(action)
        self.rewards.append(reward)
        self.probs.append(prob)
        self.dones.append(done)
        self.advs.append(adv)

    def clear(self):
        self.states = []
        self.actions = []
        self.rewards = []
        self.probs = []
        self.dones = []
        self.advs = []

    def save_memory(self):
        np.save("memory/states", np.array(self.states))
        np.save("memory/actions", np.array(self.actions))
        np.save("memory/rewards", np.array(self.rewards))
        np.save("memory/probs", np.array(self.probs))
        np.save("memory/dones", np.array(self.dones))
        np.save("memory/advs", np.array(self.advs))

    def load_memory(self):
        states = np.load("memory/states.npy")
        actions = np.load("memory/actions.npy")
        rewards = np.load("memory/rewards.npy")
        probs = np.load("memory/probs.npy")
        dones = np.load("memory/dones.npy")
        advs = np.load("memory/advs.npy")
        return states, actions, rewards, probs, dones, advs

    def normalize_advs(self):
        self.advs = ((self.advs - np.mean(self.advs)) /
                     (np.std(self.advs) + 1e-6)).tolist()

    def normalize_rewards(self):
        self.rewards = ((self.rewards - np.mean(self.rewards)) /
                        (np.std(self.rewards) + 1e-6)).tolist()


class PPO:

    def __init__(self, action_dim, k, clip_norm = None, optim = "adam",
                 write_weights = False, gamma = 0.9, eps = 0.2,
                 actor_lr = 0.0001, critic_lr = 0.0002,
                 actor_update_steps = 10, critic_update_steps = 10):
        self.action_dim = action_dim
        self.k = k
        self.gamma = gamma
        self.eps = eps
        self.actor_lr = actor_lr
        self.critic_lr = critic_lr
        self.actor_update_steps = actor_update_steps
        self.critic_update_steps = critic_update_steps
        self.actor = Actor(action_dim, k)
        self.critic = Critic()
        if optim == "adam":
            self.actor_optim = Adam(actor_lr, clipnorm=clip_norm) \
                                if clip_norm is not None else Adam(actor_lr)
            self.critic_optim = Adam(critic_lr, clipnorm=clip_norm) \
                                if clip_norm is not None else Adam(critic_lr)
        elif optim == "rms":
            self.actor_optim = RMSprop(actor_lr, clipnorm=clip_norm) \
                if clip_norm is not None else RMSprop(actor_lr)
            self.critic_optim = RMSprop(critic_lr, clipnorm=clip_norm) \
                if clip_norm is not None else RMSprop(critic_lr)
        self.actor_old = Actor(action_dim, k)
        self.write_weights = write_weights

    def choose_action(self, state):
        # currently the state should be (1, s_dim = |V| * 4)
        action_probs = self.actor.predict(state) # (1, a_dim = |E|)
        dist = tfd.Categorical(probs=action_probs)
        action = self.sample_without_replacement(action_probs, self.k) # (1, k)
        action = tf.squeeze(action)
        return action.numpy().tolist(), tf.clip_by_value(tf.squeeze(
            tf.math.reduce_prod(dist.prob(action)) / \
            (self.actor.normalizer(action_probs, self.k)).numpy() +
                                  self.actor.eps), 1e-16, 1)
        # (k,), ()

    def sample_without_replacement(self, p, k):
        z = -tf.math.log(-tf.math.log(tf.random.uniform(tf.shape(p), 0, 1)))
        z = tf.cast(z, tf.double)
        pr = tf.cast(p, tf.double)
        _, indices = tf.math.top_k(tf.math.log(pr) + z, k)
        return indices


    def get_v(self, state):
        v_tensor = self.critic(state) # (1, 1)
        return v_tensor.numpy()[0, 0]

    def update(self, memory:Memory, discounted_rewards, writer, name, step):
        self.actor_old.set_weights(self.actor.get_weights())
        # pi_old_a, pi_old = self.actor_old.evaluate_probs(memory.states,
        #                                           memory.actions)
        pi_old_a = np.array(memory.probs)
        memory_state_values = self.critic(np.array(memory.states))
        memory_state_values = tf.squeeze(memory_state_values) # (N, )
        discounted_rewards_arr = np.array(discounted_rewards)
        advantages = discounted_rewards_arr - memory_state_values

        for au in range(self.actor_update_steps):
            with tf.GradientTape() as ag:
                pi_theta_a = self.actor.evaluate_probs(memory.states,
                                                      memory.actions)
                # pi_old = tf.stop_gradient(tf.convert_to_tensor(memory.probs))
                ratios = pi_theta_a / pi_old_a  # (N,)
                surr1 = ratios * advantages
                surr2 = tf.clip_by_value(ratios, 1 - self.eps,
                                         1 + self.eps) * advantages
                actor_loss = -tf.reduce_mean(tf.minimum(surr1, surr2))
            gradient = ag.gradient(actor_loss, self.actor.trainable_variables)
            self.actor_optim.apply_gradients(
                    zip(gradient, self.actor.trainable_variables))
            #kl_div = tf.keras.losses.KLDivergence()(pi_old, pi_theta)
            #with writer.as_default():
            #    tf.summary.scalar("kl div", kl_div.numpy(), step=au)

        for cu in range(self.critic_update_steps):
            with tf.GradientTape() as cg:
                memory_state_values = self.critic(np.array(memory.states))
                memory_state_values = tf.squeeze(memory_state_values)
                advantages = discounted_rewards_arr - memory_state_values
                critic_loss = tf.reduce_mean(tf.square(advantages))

            gradient = cg.gradient(critic_loss, self.critic.trainable_variables)
            self.critic_optim.apply_gradients(
                zip(gradient, self.critic.trainable_variables))

    def init_ac(self, state):
        self.actor_old.predict(state)
        self.actor.predict(state)
        self.critic.predict(state)

    def load_ac(self, a_weights, c_weights):
        self.actor_old.load_weights(a_weights)
        self.actor.load_weights(a_weights)
        self.critic.load_weights(c_weights)


if __name__ == "__main__":
    env = gym.make("CartPole-v1")
    state_dim = env.observation_space.shape[0]
    action_dim = 2
    ppo = PPO(action_dim, 3)
    memory = Memory()
    render = False
    log_dir = "log/"
    summary_writer = tf.summary.create_file_writer(log_dir)

    all_rewards = []
    state = env.reset()
    ppo.init_old_actor(np.array([state]))

    for episode in range(MAX_EPISODE):
        state = env.reset()
        episode_reward = 0
        memory.clear()
        for t in range(MAX_STEP):
            # (1, ) (1, )
            action, prob = ppo.choose_action(np.array([state]))
            # (s_dim, )
            state_prime, reward, done, _ = env.step(action.numpy())
            memory.memorize(state, action, reward, prob, done)
            episode_reward += reward
            state = state_prime

            if (t + 1) % BATCH_SIZE == 0 or t == MAX_STEP - 1 or done:
                v_state_prime = ppo.get_v(np.array([state_prime]))
                discounted_rewards = []
                for reward, is_done in zip(reversed(memory.rewards),
                                           reversed(memory.dones)):
                    if is_done:
                        v_state_prime = 0
                    v_state_prime = reward + ppo.gamma * v_state_prime
                    discounted_rewards.append(v_state_prime)
                discounted_rewards.reverse()
                ppo.update(memory, discounted_rewards)
                memory.clear()

            if render:
                env.render()
            if done:
                break
        with summary_writer.as_default():
            tf.summary.scalar("ep reward", episode_reward, step=episode)

        if episode == 0:
            all_rewards.append(episode_reward)
        else:
            all_rewards.append(all_rewards[-1] * 0.9 + episode_reward * 0.1)
        print("Episode", episode + 1, "reward", episode_reward, end='\r')

    plt.plot(np.arange(len(all_rewards)), all_rewards)
    plt.xlabel('Episode')
    plt.ylabel('Moving averaged episode reward')
    plt.show()