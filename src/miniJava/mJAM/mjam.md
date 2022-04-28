# mJAM Guide

Info interpreted from `mJAM.Interpreter.java`.

## Basic Instructions

* args
* stack args
* stack effects

### LOAD
* d r
* (no stack args)
* pushes data[d+r] onto the stack

### LOADA
* d r
* (no stack args)
* pushes address d+r onto the stack 

### LOADI
* (no args)
* addr
* replaces addr with data[addr]

### LOADL
* d
* (no stack args)
* pushes value d onto the stack

### STORE
* d r
* val
* sets data[d+r] to be val, removes val from stack

### STOREI
* (no args)
* val addr
* sets data[addr] to be val, removes both from stack

### CALL (for code segment)
* d r (code store)
* args...
* sets LB after args; pushes current register OB, LB, CP+1 onto stack; sets new CP to d+r

### CALLI
* d r (code store)
* args... heapaddr
* sets LB after args; replaces heapaddr with current OB, pushes current LB, CP+1 onto stack; sets new CP to d+r

### CALLD
unused

### RETURN
* n d
* val? (must contain frame data (OB, LB, CP+1))
* removes d local variables; restores OB, LB, CP; if n = 1 ==> keep val on top

### PUSH
* d
* (no stack args)
* pushes d *uninitialized* values onto stack

### POP
* d
* (no stack args)
* removes d values from stack

### JUMP
* d r (code store)
* (no stack args)
* sets CP to d+r

### JUMPI
* (no args)
* codeaddr
* removes codeaddr from stack; sets CP to codeaddr

### JUMPIF
* n d r
* val
* removes val from stack; if val == n ==> sets CP to d+r

### HALT
* n
* (no stack args)
* if n <= 0 ==> stop ; else prints a snapshot!

## Primitive Instructions (call using CALL)

These only have stack arguments.
* stack args
* stack effects
* heap effects

### id
* (no stack args)
* does nothing.

### and, or, add, sub, mult, div, mod, lt, le, ge, gt, eq
* valA valB
* replaces valA and valB with op(valA, valB)

### not, neg
* val
* replaces val with op(val)

### putint, putintnl
* val
* removes val; prints val (and `\n` if nl)

### newobj
* classaddr(not used) nfields
* replaces objaddr, nfields with objaddr
* takes 2+nfields heap space: [classaddr, nfields, *objaddr:* field0..n-1]

### newarr
* size
* replaces size with arraddr
* takes 2+size heap space: [tag, size, *arraddr:* slot0..n-1] 

### arraylen
* arraddr
* replaces arraddr with size

### arrayref, fieldref
* arraddr index
* replaces arraddr, index with slot(arraddr+index)

### arrayupd, fieldupd
* arraddr index val
* removes arraddr, index, val
* sets data[arraddr + index] to val