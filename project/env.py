import numpy as np
import matplotlib.pyplot as plt


class Nodes:
    def __init__(self, num_nodes, xs=None, ys=None):
        self.num_nodes = num_nodes
        self.xs = np.random.random(num_nodes) if xs is None else xs
        self.ys = np.random.random(num_nodes) if ys is None else ys
        self.v_x = np.zeros_like(self.xs)
        self.v_y = np.zeros_like(self.xs)

    def move(self, max_a, max_v, acceleration_param=None, theta_param=None):
        assert max_a < 1
        acceleration = max_a * np.random.random(self.num_nodes) \
            if acceleration_param is None else acceleration_param
        theta = 2 * np.pi * np.random.random(self.num_nodes) \
            if theta_param is None else theta_param
        self.v_x += acceleration * np.cos(theta)
        self.v_y += acceleration * np.sin(theta)
        v_square = self.v_x ** 2 + self.v_y ** 2
        ratio = np.sqrt(v_square / max_v ** 2)
        # clip velocity
        for i in range(self.num_nodes):
            if ratio[i] > 1:
                self.v_x[i] /= ratio[i]
                self.v_y[i] /= ratio[i]

        temp_x, temp_y = self.xs + self.v_x, self.ys + self.v_y
        x_lb, x_ub = temp_x >= 0, temp_x <= 1
        y_lb, y_ub = temp_y >= 0, temp_y <= 1
        if np.prod(x_lb) * np.prod(x_ub) * np.prod(y_lb) * np.prod(y_ub):
            self.xs, self.ys = temp_x, temp_y
        else:
            for i in range(self.num_nodes):
                if temp_x[i] > 1 or temp_x[i] < 0:
                    self.xs[i] = 2 - temp_x[i] if temp_x[i] > 1 else -temp_x[i]
                    self.v_x[i] = -self.v_x[i]
                if temp_y[i] > 1 or temp_y[i] < 0:
                    self.ys[i] = 2 - temp_y[i] if temp_y[i] > 1 else -temp_y[i]
                    self.v_y[i] = -self.v_y[i]


class Field:

    def __init__(self, num_nodes, max_a, max_v):
        self.num_nodes = num_nodes
        self.nodes = Nodes(num_nodes)
        self.max_a = max_a
        self.max_v = max_v
        self.radius_square = np.log(num_nodes) / np.pi / num_nodes
        self.distance_square_mat = np.zeros((num_nodes, num_nodes))
        # self.adjacent_mat = np.zeros((self.num_nodes, self.num_nodes),
        # dtype=int)
        self.adjacent_list = [[] for _ in range(self.num_nodes)]
        self.edges = []
        self.edge_indices = []
        self.get_distance()
        self.get_edges()
        # self.cc = self.get_cc(self.adjacent_mat)
        self.cc = self.get_cc(self.adjacent_list)
        self.defensed_edges = []

    def reset(self):
        self.nodes = Nodes(self.num_nodes)
        self.get_distance()
        self.get_edges()
        self.cc = self.get_cc(self.adjacent_list)
        # self.cc = self.get_cc(self.adjacent_mat)
        self.defensed_edges = []
        return self.get_state()

    def get_state(self):
        # state = np.hstack((self.nodes.xs, self.nodes.ys,
        #                    self.nodes.v_x, self.nodes.v_y))
        state = np.zeros((self.num_nodes - 1) * self.num_nodes // 2, int)
        action = self.edge2action(self.edges)
        state[action] = 1
        return state  # (s_dim, )

    def get_distance(self):
        for i in range(self.num_nodes):
            diff_x = self.nodes.xs - self.nodes.xs[i]
            diff_y = self.nodes.ys - self.nodes.ys[i]
            self.distance_square_mat[i] = np.square(diff_x) + np.square(diff_y)


    def get_edges(self):
        # get edges in tuple of endpoints in a list
        # and in a adjacent matrix
        self.adjacent_list = [[] for _ in range(self.num_nodes)]
        self.edges = []
        self.edge_indices = []
        for i in range(self.num_nodes):
            for j in range(i + 1, self.num_nodes):
                if self.distance_square_mat[i, j] <= self.radius_square:
                    self.edges.append((i, j))
                    self.edge_indices.append(
                        i * self.num_nodes - (i + 1) * (i + 2) // 2 + j
                        )
                    self.adjacent_list[i].append(j)
                    self.adjacent_list[j].append(i)
        #            self.adjacent_mat[i, j] = 1
        #            self.adjacent_mat[j, i] = 1

    def get_cc(self, adj_list):
        # get connected components in list of lists using bfs
        cc = []
        visited = np.zeros(self.num_nodes, np.int)
        for i in range(self.num_nodes):
            if not visited[i]:
                visited[i] = not visited[i]
                queue = [i]
                cc.append([])
                while len(queue) != 0:
                    out = queue.pop(0)
                    cc[-1].append(out)
                    # for j in range(i + 1, self.num_nodes):
                    #    if adj_mat[i, j] == 1 and not visited[j]:
                    #        visited[j] = not visited[j]
                    #        queue.append(j)
                    for neighbor in adj_list[out]:
                        if not visited[neighbor]:
                            visited[neighbor] = not visited[neighbor]
                            queue.append(neighbor)
        return cc

    def next(self, acceleration=None, theta=None):
        self.nodes.move(self.max_a, self.max_v, acceleration, theta)
        self.get_distance()
        self.get_edges()
        self.cc = self.get_cc(self.adjacent_list)
        # self.cc = self.get_cc(self.adjacent_mat)
        return self.get_state()

    def defense(self, action):
        # action (k, )
        self.defensed_edges = action.copy()
        failed_defense = 0
        for edge in self.defensed_edges:
            if edge not in self.edge_indices:
                failed_defense += 1
        return failed_defense

    def attack(self, action:list):
        pun = self.remove_edges_from_adj_list(self.adjacent_list, action)
        new_n_cc = len(self.get_cc(self.adjacent_list))
        reward = new_n_cc - len(self.cc)
        return reward, pun

    def remove_edges_from_adj_list(self, adj_list, action: list):
        failed_remove = 0
        edges = self.action2edge(action)
        for (edge, edge_idx) in zip(edges, action):
            if edge_idx not in self.defensed_edges and \
                    edge_idx in self.edge_indices:
                adj_list[edge[0]].remove(edge[1])
                adj_list[edge[1]].remove(edge[0])
            elif edge_idx not in self.edge_indices:
                failed_remove += 1
        return failed_remove

    def action2edge(self, action: list):
        # action k of length k
        edges = []
        for i in range(len(action)):
            start = 0
            end = self.num_nodes - 2
            mid = (self.num_nodes - 1) // 2
            while True:
                mid_begin = mid * self.num_nodes - mid * (mid + 1) // 2
                mid_p1_begin = (mid + 1) * self.num_nodes - \
                               (mid + 1) * (mid + 2) // 2
                if mid_begin <= action[i] and mid_p1_begin > action[i]:
                    row = mid
                    col = action[i] - mid_p1_begin + self.num_nodes
                    edges.append((row, col))
                    break
                elif mid_begin <= action[i] and \
                    mid_p1_begin <= action[i]:
                    start = mid + 1
                    mid = (start + end) // 2
                else:
                    end = mid
                    mid = (start + end) // 2
        return edges

    def edge2action(self, edges: list):
        action = []
        for edge in edges:
            action.append(edge[0] * self.num_nodes -
                          (edge[0] + 1) * (edge[0] + 2) // 2 + edge[1])
        return action

    def print(self, eager=False):
        plt.figure(figsize=(6, 6))
        plt.xlim(0, 1)
        plt.ylim(0, 1)
        plt.plot(np.sqrt(self.radius_square), 0, marker = "o")
        plt.annotate("$r_t$", xy=(np.sqrt(self.radius_square), 0),
                     xytext=(np.sqrt(self.radius_square) - 0.01, 0.02))
        for i in range(self.num_nodes):
            plt.plot(self.nodes.xs[i], self.nodes.ys[i], "mo")
            plt.annotate(str(i), xy=(self.nodes.xs[i], self.nodes.ys[i]),
                         xytext=(self.nodes.xs[i] + 0.02,
                                 self.nodes.ys[i] - 0.02))
        for endpoints in self.edges:
            plt.plot([self.nodes.xs[endpoints[0]], self.nodes.xs[endpoints[1]]],
                     [self.nodes.ys[endpoints[0]], self.nodes.ys[endpoints[1]]],
                     linestyle="-", color="c")
        if eager:
            plt.show()


def print_mat(N):
    print(end=" ")
    for i in range(N):
        print("  " + str(i), end = "")
    print()
    for i in range(N):
        print(str(i), end="")
        print(" " * (3 * i + 3), end="")
        for j in range(i + 1, N):
            num = i * N - (i + 1) * (i + 2) // 2 + j
            num_digits = len(str(num))
            print(" " * (3 - num_digits) + str(num), end = "")
        print()

if __name__ == "__main__":
    F_d = 8
    E_a = 1 / F_d
    field = Field(10, 0.01, 0.1)
    field.reset()
    while True:
        field.print(True)
        field.next()
    #plt.ion()
    #plt.figure(figsize=(6, 6))
    #i = 0
    #iss = []
    #for t in range(10):
        #plt.xlim(0, 1)
        #plt.ylim(0, 1)
        #plt.title("t = " + str(t) + " #cc:" + str(len(field.cc)))
        #field.print()
        #if t % F_d == 0:
            #edges = eval(input("edges to defense: "))
        #    edges = ((0, 1), (0, 2))
        #    field.set_defensed_edges(edges)

        #if np.random.random() < E_a * np.e ** (-E_a):
            #edges = eval(input("edges to attack: "))
        #    edges = ((0, 1), (0, 2))
        #    r = field.attack(edges)
            #print("real adj mat", field.adjacent_list)
            #print("defensed edges", field.defensed_edges)
            #print("reward", r)
            #command = input("press any to resume")
        #    iss.append(i)
        #    i = 0
        #else:
        #    i += 1
    #    if t % F_d == 0:
    #        adj_mats.append(field.adjacent_mat)

    #    field.next()
        #plt.pause(0.1)
        #plt.clf()
    #print(len(iss),np.average(iss))

    #plt.show()
    #plt.close()