import numpy as np
import matplotlib.pyplot as plt

class Nodes:
    def __init__(self, num_nodes, xs = None, ys = None):
        self.num_nodes = num_nodes
        self.xs = np.random.random((num_nodes)) if xs is None else xs
        self.ys = np.random.random((num_nodes)) if ys is None else ys
        self.v_x = np.zeros_like(self.xs)
        self.v_y = np.zeros_like(self.xs)

    def move(self, max_a, max_v, acceleration = None, theta = None):
        assert max_a < 1
        self.acceleration = max_a * np.random.random((self.num_nodes)) if acceleration is None else acceleration
        self.theta = 2 * np.pi * np.random.random((self.num_nodes)) if theta is None else theta
        self.v_x += self.acceleration * np.cos(self.theta)
        self.v_y += self.acceleration * np.sin(self.theta)
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
        if np.prod(x_lb) * np.prod(x_ub) * np.prod(y_lb) * np.prod(y_ub) == True:
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
        self.get_distance()
        self.get_edges()
        self.get_cc()

    def get_distance(self):
        for i in range(self.num_nodes):
            diff_x = self.nodes.xs - self.nodes.xs[i]
            diff_y = self.nodes.ys - self.nodes.ys[i]
            self.distance_square_mat[i] = np.square(diff_x) + np.square(diff_y)

    def get_edges(self):
        # get edges in tuple of endpoints in a list
        # and in a adjacent matrix
        self.edges = []
        self.adjacent_mat = [[] for _ in range(self.num_nodes)]
        for i in range(self.num_nodes):
            for j in range(i + 1, self.num_nodes):
                if self.distance_square_mat[i, j] <= self.radius_square:
                    self.edges.append((i, j))
                    self.adjacent_mat[i].append(j)
                    self.adjacent_mat[j].append(i)

    def get_cc(self):
        # get connected components in list of lists using bfs
        self.cc = []
        visited = np.zeros(self.num_nodes, np.int)
        for i in range(self.num_nodes):
            if not visited[i]:
                visited[i] = not visited[i]
                queue = [i]
                self.cc.append([])
                while len(queue) != 0:
                    out = queue.pop(0)
                    self.cc[-1].append(out)
                    for neighbor in self.adjacent_mat[out]:
                        if not visited[neighbor]:
                            visited[neighbor] = not visited[neighbor]
                            queue.append(neighbor)
        return len(self.cc)


    def next(self, acceleration = None, theta = None):
        self.nodes.move(self.max_a, self.max_v, acceleration, theta)
        self.get_distance()
        self.get_edges()
        self.get_cc()

    def defense(self, edges):
        pass

    def attack(self, edges):
        pass

    def print(self):
        plt.plot(np.sqrt(self.radius_square), 0, marker = "o")
        for i in range(self.num_nodes):
            plt.plot(self.nodes.xs[i], self.nodes.ys[i], marker = "+")
        for endpoints in self.edges:
            plt.plot([self.nodes.xs[endpoints[0]], self.nodes.xs[endpoints[1]]],
                     [self.nodes.ys[endpoints[0]], self.nodes.ys[endpoints[1]]])

if __name__ == "__main__":
    field = Field(10, 0.01, 0.1)
    #plt.ion()
    #plt.figure(figsize=(6, 6))
    for _ in range(10):
    #    plt.xlim(0, 1)
    #    plt.ylim(0, 1)
    #    plt.title("#cc:" + str(len(field.cc)))
    #    field.print()
        print(field.edges)
        print(field.adjacent_mat)
        print(field.cc)
        print()
        field.next()
    #    plt.pause(0.5)
    #    plt.clf()
    #plt.show()
    #plt.close()