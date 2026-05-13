import os
result_file = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'test_result.txt')
with open(result_file, 'w') as f:
    f.write("hello from python\n")
