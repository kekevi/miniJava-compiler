  0         LOADL        0
  1         CALL         newarr  
  2         CALL         L10
  3         HALT   (0)   
  4         PUSH         1
  5  L10:   PUSH         1
  6         LOADL        0
  7         LOADL        1
  8         CALL         newobj  
  9         STORE        3[LB]
 10         LOADA        3[LB]
 11         LOADA        3[LB]
 12         CALLI        L12
 13         PUSH         1
 14         LOADL        2
 15         LOADA        3[LB]
 16         LOADA        3[LB]
 17         CALLI        L11
 18         STORE        4[LB]
 19         LOADA        0[SB]
 20         CALL         putintnl
 21         PUSH         1
 22         LOADA        0[SB]
 23         STORE        5[LB]
 24         LOADA        5[LB]
 25         CALL         putintnl
 26         PUSH         1
 27         LOADA        4[LB]
 28         LOADA        4[LB]
 29         CALLI        L13
 30         STORE        6[LB]
 31         LOADA        6[LB]
 32         CALL         putintnl
 33         PUSH         1
 34         LOADL        0
 35         LOADL        0
 36         CALL         newobj  
 37         STORE        7[LB]
 38         RETURN (0)   1
 39         PUSH         1
 40         LOADL        1
 41         STORE        3[LB]
 42         LOADA        3[LB]
 43         LOADL        1
 44         CALL         add     
 45         LOADA        3[LB]
 46         STOREI 
 47         LOADA        3[LB]
 48         RETURN (1)   0
 49         RETURN (0)   0
 50  L11:   LOADA        0[SB]
 51         LOADA        -1[LB]
 52         CALL         add     
 53         LOADA        0[SB]
 54         STOREI 
 55         LOADA        0[SB]
 56         RETURN (1)   1
 57         RETURN (0)   1
 58  L12:   LOADL        0
 59         LOADL        1
 60         CALL         newobj  
 61         LOADA        0[SB]
 62         STOREI 
 63         RETURN (0)   0
 64  L13:   LOADA        0[SB]
 65         LOADL        1
 66         CALL         add     
 67         RETURN (1)   0
 68         RETURN (0)   0
