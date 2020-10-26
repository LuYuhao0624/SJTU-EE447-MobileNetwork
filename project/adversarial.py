from ppo import PPO, Memory
from env import Field
import tensorflow as tf
import numpy as np

if __name__ == "__main__":
    N = 10
    max_a = 0.01
    max_v = 0.1
    k = 3
    MAX_EP_A = 100000
    MAX_EP_D = 100000
    BATCH_SIZE = 32
    log_dir = "log/adversarial"
    weights_dir = "weights/adversarial/"
    rewards_dir = "rewards/adversarial/"
    summary_writer = tf.summary.create_file_writer(log_dir)

    field = Field(N, max_a, max_v)
    defender = PPO(N * (N - 1) // 2, k, actor_lr=1e-4, critic_lr=1e-4)
    attacker = PPO(N * (N - 1) // 2, k, actor_lr=1e-4, critic_lr=1e-4)
    defender_memory = Memory()
    attacker_memory = Memory()
    defender_rewards = []
    attacker_rewards = []
    state = field.reset()  # (1, s_dim)
    defender.init_ac(np.array([state]))
    attacker.init_ac(np.array([state]))
    running_mean_a = []
    running_mean_d = []
    all_rewards_a = []
    all_rewards_d = []

    for i in range(100):
        # fix defender, train attacker
        print("Training attacker ... iter", i + 1)
        for ep in range(MAX_EP_A):
            state = field.get_state()
            action_d, prob_d = defender.choose_action(np.array([state]))
            pun_d = field.defense(action_d)
            r_d = -pun_d

            action_a, prob_a = attacker.choose_action(np.array([state]))
            reward_a, pun_a = field.attack(action_a)
            r_a = reward_a - pun_a
            r_d -= reward_a

            attacker_memory.memorize(
                state, action_a, r_a, prob_a, True, None
            )
            running_mean_d.append(r_d)
            running_mean_a.append(r_a)

            state = field.next()

            if (ep + 1) % BATCH_SIZE == 0 or ep == MAX_EP_A - 1:
                attacker_memory.normalize_rewards()
                attacker.update(attacker_memory, attacker_memory.rewards,
                                summary_writer, "attacker", ep)
                attacker_memory.clear()

            if (ep + 1) % 1000 == 0:
                mean_reward_a = np.mean(running_mean_a)
                mean_reward_d = np.mean(running_mean_d)
                all_rewards_a.append(mean_reward_a)
                all_rewards_d.append(mean_reward_d)
                np.save(rewards_dir + "attacker", all_rewards_a)
                np.save(rewards_dir + "defender", all_rewards_d)
                print("avg reward " + str(ep - 999) + "-" + str(ep + 1) + "/" +
                      str(MAX_EP_A) +
                      " eps. defender:", np.round(mean_reward_d, 4),
                      " attacker:", np.round(mean_reward_a, 4))
                running_mean_a = []
                running_mean_d = []

            if (ep + 1) % (MAX_EP_A // 2) == 0:
                attacker.actor.save_weights(
                    weights_dir + "attacker_actor_it" + str(i + 1) +
                    "_ep" + str(ep + 1) + ".h5"
                )
                attacker.critic.save_weights(
                    weights_dir + "attacker_critic_it" + str(i + 1) +
                    "_ep" + str(ep + 1) + ".h5"
                )

        print("Training defender ... iter", i + 1)
        for ep in range(MAX_EP_D):
            state = field.get_state()
            action_d, prob_d = defender.choose_action(np.array([state]))
            pun_d = field.defense(action_d)
            r_d = -pun_d

            action_a, prob_a = attacker.choose_action(np.array([state]))
            reward_a, pun_a = field.attack(action_a)
            r_a = reward_a - pun_a
            r_d -= reward_a

            defender_memory.memorize(
                state, action_d, r_d, prob_d, True, None
            )
            running_mean_d.append(r_d)
            running_mean_a.append(r_a)

            state = field.next()

            if (ep + 1) % BATCH_SIZE == 0 or ep == MAX_EP_D - 1:
                defender_memory.normalize_rewards()
                defender.update(defender_memory, defender_memory.rewards,
                                summary_writer, "defender", ep)
                defender_memory.clear()

            if (ep + 1) % 1000 == 0:
                mean_reward_a = np.mean(running_mean_a)
                mean_reward_d = np.mean(running_mean_d)
                all_rewards_a.append(mean_reward_a)
                all_rewards_d.append(mean_reward_d)
                np.save(rewards_dir + "attacker", all_rewards_a)
                np.save(rewards_dir + "defender", all_rewards_d)
                print("avg reward " + str(ep - 999) + "-" + str(ep + 1) + "/" +
                      str(MAX_EP_D) +
                      " eps. defender:", np.round(mean_reward_d, 4),
                      " attacker:", np.round(mean_reward_a, 4))
                running_mean_a = []
                running_mean_d = []

            if (ep + 1) % (MAX_EP_D // 2) == 0:
                defender.actor.save_weights(
                    weights_dir + "defender_actor_it" + str(i + 1) +
                    "_ep" + str(ep + 1) + ".h5"
                )
                defender.critic.save_weights(
                    weights_dir + "defender_critic_it" + str(i + 1) +
                    "_ep" + str(ep + 1) + ".h5"
                )



