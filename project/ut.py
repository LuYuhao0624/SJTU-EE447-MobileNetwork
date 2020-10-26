import time
import sympy as s
import numpy as np

def X(k, fs):
    previous_X = [1]
    for n in range(1, k + 1):
        res = 0
        sign = 1
        for i in range(1, n + 1):
            res += sign * previous_X[-i] * fs[i]
            sign = -sign
        previous_X.append(res / n)
    return previous_X

fs = s.symbols("f0:{}".format(10))
Xs = X(8, fs)
for item in Xs:
    print(s.expand(item))
################################################################################
# sympy tests #
################################################################################
# def f(vs, m, n):
#     res = 0
#     for v in vs:
#         res += v ** m
#     return res ** n
#
# def g(vs, n):
#     res = 0
#     for v in vs:
#         res += v ** n
#     return res
#
# def h(v: np.ndarray, n: int):
#     return np.sum(np.power(v, n))
#
# size = 1000000
# p = np.random.random(size)
#
# start_h = time.time()
# cross3rd_h = (h(p, 1) ** 3 - 3 * h(p, 1) * h(p, 2) + 2 * h(p, 3)) / 6
# end_h = time.time()
# cost_h = end_h - start_h
# print(cost_h)

# start_i = time.time()
# cross3rd_i = 0
# for i in range(p.shape[0]):
#     for j in range(i + 1, p.shape[0]):
#         for k in range(j + 1, p.shape[0]):
#             cross3rd_i += p[i] * p[j] * p[k]
# end_i = time.time()
# cost_i = end_i - start_i
#
# print(cost_i)
# print(cross3rd_h - cross3rd_i)


# p = [
#     0.02165089, 0.02142955, 0.02180815, 0.01741156, 0.02764132, 0.02062072,
#     0.01198688, 0.01993877, 0.02308781, 0.0189144, 0.0272406, 0.02235609,
#     0.02831758, 0.02112716, 0.02491532, 0.01756603, 0.01653074, 0.02148617,
#     0.02190716, 0.01204671, 0.02400111, 0.01830614, 0.0214888, 0.03915779,
#     0.0166879,  0.02303439, 0.02190591, 0.01627246, 0.03215544, 0.01953647,
#     0.02581961, 0.01725175, 0.01726201, 0.02013829, 0.02303798, 0.02757808,
#     0.02397816, 0.03185833, 0.02513914, 0.02673717, 0.02304502, 0.01811953,
#     0.02982352, 0.02881872, 0.0108627
#     ]
#
# res = 0
# for i in range(len(p)):
#     for j in range(i + 1, len(p)):
#         for k in range(j + 1, len(p)):
#             res += p[i] * p[j] * p[k]
# print(res * 6 / (f(p, 1, 3) + 2 * f(p, 3, 1) - 3 * f(p, 2, 1) * f(p, 1, 1)))
#
# a, b, c, d, e = s.symbols("a b c d e")
# vs = [a, b, c, d, e]
# f1 = f(vs, 1, 4)
# g1 = f(vs, 4, 1)
# g2 = f(vs, 3, 1) * f(vs, 1, 1) - f(vs, 4 ,1)
# g3 = f(vs, 2, 2) - g1
# g4 = f(vs, 2, 1) * f(vs, 1, 2) - g1 - 2 * g2 - g3
#
# cross1st_f = f(vs, 1, 1)
# cross1st_g = g(vs, 1)
# cross2nd_f = (cross1st_f * f(vs, 1, 1) - f(vs, 2, 1)) / 2
# cross2nd_g = (g(vs, 1) ** 2 - g(vs, 2)) / 2
# print("2nd order cross terms by f:\n", s.expand(cross2nd_f))
# print("2nd order cross terms by g:\n", s.expand(cross2nd_g))
# cross3rd_f = (cross2nd_f * f(vs, 1, 1) - f(vs, 2, 1) * cross1st_f +
#             f(vs, 3, 1)) / 3
# cross3rd_g = (g(vs, 1) ** 3 - 3 * g(vs, 1) * g(vs, 2) + 2 * g(vs, 3)) / 6
# print("3rd order cross terms by f:\n", s.expand(cross3rd_f))
# print("3rd order cross terms by g:\n", s.expand(cross3rd_g))
# cross4th_f = (cross3rd_f * f(vs, 1, 1) - cross2nd_f * f(vs, 2, 1) +
#             cross1st_f * f(vs, 3, 1) - f(vs, 4, 1)) / 4
# print("4th order cross terms by f:", s.expand(cross4th_f))

#
# print("\n",s.expand(f1 - g1 - 4 * g2 - 3 * g3 - 6 * g4))
# print("\n",s.expand((f(vs, 1, 4) - 6 * f(vs, 4, 1) +
#                     8 * f(vs, 3, 1) * f(vs, 1, 1) + 3 * f(vs, 2, 2)
#                     - 6 * f(vs, 2, 1) * f(vs, 1, 2)) / 24))


