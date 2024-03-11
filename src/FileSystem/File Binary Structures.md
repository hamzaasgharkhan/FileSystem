### Binary Structures of Files

    MAGIC_VALUE                 -    4 bytes (0x61717561)
    The Preset Filenames are encoded in ASCII.
    User filenames are encoded in UTF-8

#### SuperBlock
    Magic Value                 -       4 bytes                                 || Starting Index: 0
    Flags                       -       1 byte                                  || Starting Index: 4
    FileSystem Name (UTF-8)     -       256 bytes                               || Starting Index: 5
    Magic Value                 -       4 bytes                                 || Starting Index: 261
    Total DirectoryStores       -       8 bytes                                 || Starting Index: 265
    Total DataStores            -       8 bytes                                 || Starting Index: 273     
    Total ThumbnailStores       -       8 bytes                                 || Starting Index: 281
    Total INodeStores           -       8 bytes                                 || Starting Index: 289
    Total ExtentStores          -       8 Bytes                                 || Starting Index: 297
    Total AttributeStores       -       8 Bytes                                 || Starting Index: 305
    Magic Value                 -       4 bytes                                 || Starting Index: 313
    
    Size: 317 Bytes
    
#### INode Entry
    Magic Value                 -       4 bytes                                 || Starting Index: 0
    iNodeAddress                -       8 bytes                                 || Starting Index: 4
    parentINodeAddress          -       8 bytes                                 || Starting Index: 12
    size                        -       8 bytes                                 || Starting Index: 20
    flags                       -       1 byte                                  || Starting Index: 28
    creationTime                -       8 bytes                                 || Starting Index: 29
    lastModifiedTime            -       8 bytes                                 || Starting Index: 37
    Magic Value                 -       4 bytes                                 || Starting Index: 45
    extentStoreAddress          -       8 bytes                                 || Starting Index: 49
    extentCount                 -       8 bytes                                 || Starting Index: 57
    thumbnailStoreAddress       -       8 bytes                                 || Starting Index: 65
    Magic Value                 -       4 bytes                                 || Starting Index: 73
    
    Size: 77 bytes



#### DirectoryStore Entry
    Magic Value                 -       4 bytes                                 || Starting Index: 0
    index                       -       8 bytes                                 || Starting Index: 4
    parentIndex                 -       8 bytes                                 || Starting Index: 12
    previousSiblingIndex        -       8 bytes                                 || Starting Index: 20
    nextSiblingIndex            -       8 bytes                                 || Starting Index: 28
    childIndex                  -       8 bytes                                 || Starting Index: 36
    iNode                       -       8 bytes                                 || Starting Index: 44
    parentINode                 -       8 bytes                                 || Starting Index: 52
    flags                       -       1 byte                                  || Starting Index: 60
    Magic Value                 -       4 bytes                                 || Starting Index: 61
    name                        -       256 bytes                               || Starting Index: 65
    Magic Value                 -       4 bytes                                 || Starting Index: 321

    Size: 325 bytes

#### ExtentStore Entry
    Magic Value                 -       4 bytes                                 || Starting Index: 0
    DataStore Index             -       4 bytes                                 || Starting Index: 4
    DataStore Offset            -       4 bytes                                 || Starting Index: 8
    Length                      -       4 bytes                                 || Starting Index: 12 

    Size: 16 Bytes

#### DataStore Block
    MD5 hash                    -      16 bytes                                 || Starting Index: 0
    Bytes Occupied              -       2 bytes                                 || Starting Index: 16
    Block Flags                 -       1 byte                                  || Starting Index: 18
    Block Bitmap                -       453 bytes                               || Starting Index: 19
    Data Bytes                  -       3624 bytes                              || Starting Index: 472

    Size: 4096 bytes

#### DataStore Entry
    Just plain data is pasted.

#### ThumbnailStore Header
    Magic Value                 -       4 bytes                                 || Starting Index: 0
    ThumbnailStore Number       -       4 bytes                                 || Starting Index: 4
    flags                       -       1 byte                                  || Starting Index: 8
    
    Size: 9 bytes

#### ThumbnailStore Entry
    TO BE WRITTEN

### FLAGS

#### NODE
    RUNTIME_FLAGS
    _____________
    CNR     -       Children Not Read : Set to true if children of the Node have not been read from disk.