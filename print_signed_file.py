def print_signed_integers(file_path, index, end_index, integers_per_line=16):
    with open(file_path, 'rb') as f:
        integers_printed = 0
        i = 0
        while True:
            # Read a single byte from the file
            byte_data = f.read(1)
            if i < index:
                i += 1
                continue
            
            if i > end_index:
                break             
            # If no more bytes are left, break the loop
            if not byte_data:
                break
            
            # Interpret byte as a signed integer
            integer_value = int.from_bytes(byte_data, byteorder='big', signed=True)
            
            # Print the integer value
            print(integer_value, end=" ")
            integers_printed += 1
            i += 1
            # Start a new line if the desired number of integers per line is reached
            if integers_printed % integers_per_line == 0:
                print()

# Example usage: provide the file path as a command-line argument
import sys

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python script.py <file_path>")
        print("Usage: python script.py <file_path> starting_index")
        print("Usage: python script.py <file_path> starting_index ending_index")
    else:
        file_path = sys.argv[1]
        if len(sys.argv) > 2:
            index = int(sys.argv[2])
        else:
            index = 0
        if len(sys.argv) > 3:
            end_index = int(sys.argv[3])
        else:
            end_index = -1
        print_signed_integers(file_path, index, end_index)
        print("");
