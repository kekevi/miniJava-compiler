  0         LOADL        0
  1         CALL         newarr  
  2         CALL         L10
  3         HALT   (0)   
  4  L10:   PUSH         1
  5         LOADL        23
  6         LOADL        2
  7         CALL         neg     
  8         CALL         add     
  9         STORE        3[LB]
 10         LOADA        3[LB]
 11         CALL         putintnl
 12         RETURN (0)   1
 13         RETURN (0)   1
