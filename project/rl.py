import torch
import numpy as np
import torch.nn as nn
import torch.nn.functional as f
import torch.distributions as d

device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")

class Actor(nn.Module):

    def __init__(self, state_dim, action_dim):
        super().__init__()
        self.linear1 = nn.Linear(state_dim, 128)
        self.linear2 = nn.Linear(128, 256)
        self.linear3 = nn.Linear(256, action_dim)

    def forward(self, state):
        output = f.relu(self.linear1(state))
        output = f.relu(self.linear2(output))
        output = f.softmax(self.linear3(output))
        return output


class Critic(nn.Module):

    def __init__(self, state_dim):
        super().__init__()
        self.linear1 = nn.Linear(state_dim, 128)
        self.linear2 = nn.Linear(128, 256)
        self.linear3 = nn.Linear(256, 1)

    def forward(self, state):
        output = f.relu(self.linear1(state))
        output = f.relu(self.linear2(output))
        output = self.linear3(output)
        return output


class PPO:

    def __init__(self, state_dim, action_dim, gamma, epoch, eps = 0.2, lr = 0.001, beta = (0.9, 0.999),
                 action_update_steps = 10, critic_update_steps = 10):
        self.state_dim = state_dim
        self.action_dim = action_dim
        self.gamma = gamma
        self.epoch = epoch
        self.eps = eps
        self.lr = lr
        self.beta = beta
        self.action_update_steps = action_update_steps
        self.critic_update_steps = critic_update_steps
        self.actor = Actor(state_dim, action_dim).to(device)
        self.critic = Critic(state_dim).to(device)
        self.actor_optim = torch.optim.Adam(self.actor.parameters(), self.lr, self.beta)
        self.critic_optim = torch.optim.Adam(self.critic.parameters(), self.lr, self.beta)
        self.actor_old = Actor(state_dim, action_dim).to(device)
        self.actor_old.load_state_dict(self.actor.state_dict())
        self.loss = None

    def update(self, s, a, r):
        s = torch.from_numpy(np.vstack(s)).to(device).detach()
        a = torch.from_numpy(np.vstack(a)).to(device).detach()
        r = torch.from_numpy(np.vstack(r)).to(device).detach()
        assert s.requires_grad == False and a.requires_grad == False and \
               r.requires_grad == False
        # update the parameter in old actor network
        self.actor_old.load_state_dict(self.actor.state_dict())

        # get the advantage value
        advantage = r - self.critic(s)

        # update actor, using clipping method
        for _ in range(self.action_update_steps):
            prob_a, prob_a_old = self.actor(s), self.actor_old(s)
            dist, dist_old = d.Categorical(prob_a), d.Categorical(prob_a_old)
            log_prob_a, log_prob_a_old = dist.log_prob(a), dist_old.log_prob(a)
            ratio = torch.exp(log_prob_a - log_prob_a_old)
            surr1 = ratio * advantage
            surr2 = torch.clamp(ratio, 1 - self.eps, 1 + self.eps) * advantage
            actor_loss = -torch.min(surr1, surr2)

            # take gradient
            self.actor_optim.zero_grad()
            actor_loss.mean().backward()
            self.actor_optim.step()

        # update critic
        for _ in range(self.critic_update_steps):
            advantage = r - self.critic(s)
            critic_loss = torch.pow(advantage, 2)

            # take gradient
            self.critic_optim.zero_grad()
            critic_loss.mean().backward()