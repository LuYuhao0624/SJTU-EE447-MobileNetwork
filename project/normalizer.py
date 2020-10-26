import numpy as np
import sympy as s
import time
import matplotlib.pyplot as plt
from scipy.special import  erf
from tensorflow.keras.layers import Dense
import argparse

def f(p, m):
    return np.sum(np.power(p, m))

def compute_normalizer_sympy(N, k, E):
    num_edges = N * (N - 1) // 2 if E is None else E
    p = s.symbols("p1:" + str(num_edges + 1))
    p = np.array(p)
    print("p =", p)
    print("X_" + str(k) + "(p) =", s.expand(compute_normalizer(p, k)))

def compute_normalizer_old(p, k):
    cross = [1]
    for n in range(1, k + 1):
        res = 0
        sign = 1
        for i in range(1, n + 1):
            res += sign * cross[-i] * f(p, i)
            sign = -sign
        cross.append(res / n)
    return cross[-1]

def compute_normalizer(p, k):
    cross = [1]
    fs = [0]
    for i in range(1, k + 1):
        fs.append(f(p, i))
    for n in range(1, k + 1):
        res = 0
        sign = 1
        for i in range(1, n + 1):
            res += sign * cross[-i] * fs[i]
            sign = -sign
        cross.append(res / n)
    return cross[-1]


def compute_normalizer_it(p, k):
    len = p.shape[0]
    res = 0
    if k == 1:
        return np.sum(p)
    elif k == 2:
        for i in range(len - 1):
            for j in range(i + 1, len):
                res += p[i] * p[j]
    elif k == 3:
        for i in range(len - 2):
            for j in range(i + 1, len - 1):
                for k in range(j + 1, len):
                    res += p[i] * p[j] * p[k]
    elif k == 4:
        for i in range(len - 3):
            for j in range(i + 1, len - 2):
                for k in range(j + 1, len - 1):
                    for l in range(k + 1, len):
                        res += p[i] * p[j] * p[k] * p[l]
    elif k == 5:
        for i in range(len - 4):
            for j in range(i + 1, len - 3):
                for k in range(j + 1, len - 2):
                    for l in range(k + 1, len - 1):
                        for m in range(l + 1, len):
                            res += p[i] * p[j] * p[k] * p[l] * p[m]
    else:
        raise NotImplementedError("Not implemented.")
    return res

def relu(p, k):
    return np.maximum(0, p)

def softmax(p, k):
    return np.exp(p) / np.sum(np.exp(p))

def gelu(p, k):
    return p * 0.5 * (1.0 + erf(p / np.sqrt(2.0)))

def dense(p, k):
    Dense(k)(p)

def get_time(func, p, k, it, expand=False):
    if expand:
        p = p[np.newaxis,]
    start = time.time()
    for i in range(it):
        func(p, k)
    end = time.time()
    duration = (end - start) / it
    return duration

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--N", "-N", type=int, default=None)
    parser.add_argument("--k", "-k", type=int, default= None)
    parser.add_argument("--E", "-E", type=int, default=None)
    args = parser.parse_args()
    assert args.N is not None or args.E is not None
    assert args.k is not None

    compute_normalizer_sympy(args.N, args.k, args.E)







    # result = np.zeros(5)
    # for i in range(5):
    #     p = np.random.random(10 ** (i + 2))
    #     duration = get_time(dense, p, 0, 10, True)
    #     result[i] = duration
    # print(result)
    # np.save("results/dense", result)
    # result = np.load("results/non_iter.npy")
    # result_relu = np.load("results/relu.npy")
    # result_softmax = np.load("results/softmax.npy")
    # result_gelu = np.load("results/gelu.npy")
    # result_dense = np.load("results/dense.npy")
    # xs = [i + 2 for i in range(5)]
    # plt.xlabel("number of entries $\lg N$")
    # plt.ylabel("time $\lg T$")
    # plt.title("time comparison between activation functions, layers and our "
    #           "approach")
    # for i in range(result.shape[0]):
    #     ys = np.log10(result[i])
    #     plt.plot(xs, ys, label="k = " + str(i * 5 + 5))
    # plt.plot(xs, np.log10(result_relu), label="relu")
    # plt.plot(xs, np.log10(result_softmax), label="softmax")
    # plt.plot(xs, np.log10(result_gelu), label="gelu")
    # plt.plot(xs, np.log10(result_dense), label="dense")
    # plt.legend()
    # plt.show()
    # result = np.zeros((4, 6))
    # for k in range(4):
    #     for i in range(6):
    #         p = np.random.random(10 ** (i + 1))
    #         if i + k <= 3:
    #             duration = get_time(compute_normalizer, p, k * 5 + 5, 100)
    #         else:
    #             duration = get_time(compute_normalizer, p, k * 5 + 5, 5)
    #         result[k, i] = duration
    #         print("k =\t", k, "\ti = \t", i, end="\r")
    # print(result)
    # np.save("results/non_iter", result)
    # print(np.log(result))
    # res2, duration2 = get_time(compute_normalizer_it, p, k)
    # print("ratio", res1 / res2)
    # print("d1", duration1)
    # print("d2", duration2)

    #########################################################
    # plot comparison between iter-based and non-iter-based #
    #########################################################
    # result_iter = np.load("results/iter_comp.npy")
    # result_non_iter = np.load("results/non_iter_comp.npy")
    # xs = [20 * i + 20 for i in range(6)]
    # color = ["b", "r", "m", "c"]
    # plt.ylim(-5, 5)
    # plt.xlabel("number of entries $N$")
    # plt.ylabel("time $\log T$")
    # plt.title("comparison of iteration-based (dashed) and our approach (solid)")
    # for i in range(4):
    #     ys = np.log10(result_iter[i])
    #     plt.plot(xs, ys, color=color[i], linestyle="--")
    #     ys = np.log10(result_non_iter[i])
    #     plt.plot(xs, ys, color=color[i], label="k = " + str(i + 2))
    # plt.legend()
    # plt.show()
