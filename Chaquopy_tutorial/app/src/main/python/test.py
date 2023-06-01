import numpy as np
def main():
    arr = np.array([2, 3, 4])
    arr = np.array2string(arr, precision=2, separator=',',
                          suppress_small=True)
    return arr