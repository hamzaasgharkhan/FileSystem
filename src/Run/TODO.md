# TODO

-[ ] Create a method to take an array of bytes and return an MD5 hash for that array of bytes.
-[ ] Also add the option in that method to exclude a particular range of indices.
-[ ] The method should return the hashes in byte arrays.
-[ ] Add md5 hashes to all stores. 
-[ ] Fix directory store methods after adding md5 hashes
-[ ] Use Path object to get files to make the process platform independent
-[ ] Implement thumbnail mechanism
-[ ] Use BufferedReader to make things faster or add internal buffers to the classes.
-[ ] Use LinkedLists in all files to make them faster yet better at storing data. There can be several linkedlists within a single file.
-[ ] Write unit tests for all functionalities 


# Future Iterations / Optimizations
- [ ] Use SQLite to handle all the basic tedious information. It will make the bulk of operations easier and much more efficient.
