  0         LOADL        0
  1         CALL         newarr  
  2         CALL         L10
  3         HALT   (0)   
  4  L10:   LOADL        1337
  5         LOADL        1
  6         STORE        3[LB]
  7         LOADA        3[LB]
  8         LOADI  
  9         CALL         putintnl
 10         LOADL        2
 11         LOADA        3[LB]
 12         LOADI  
 13         CALL         mult    
 14         LOADA        3[LB]
 15         LOADI  
 16         CALL         add     
 17         LOADL        1
 18         CALL         sub     
 19         LOADA        3[LB]
 20         STOREI 
 21         LOADA        3[LB]
 22         LOADI  
 23         CALL         putintnl
 24         LOADL        3
 25         CALL         putintnl
 26         LOADA        3[LB]
 27         LOADI  
 28         LOADL        1
 29         CALL         neg     
 30         CALL         ne      
 31         JUMPIF (0)   L11
 32         LOADL        4
 33         CALL         putintnl
 34         JUMP         L12
 35  L11:   LOADL        1
 36         CALL         neg     
 37         CALL         putintnl
 38  L12:   LOADL        1337
 39         LOADL        0
 40         STORE        4[LB]
 41  L13:   LOADA        4[LB]
 42         LOADI  
 43         LOADL        5
 44         CALL         lt      
 45         JUMPIF (0)   L14
 46         LOADA        4[LB]
 47         LOADI  
 48         LOADL        1
 49         CALL         add     
 50         LOADA        4[LB]
 51         STOREI 
 52         LOADA        4[LB]
 53         LOADI  
 54         LOADA        3[LB]
 55         STOREI 
 56         POP          0
 57         JUMP         L13
 58  L14:   LOADA        3[LB]
 59         LOADI  
 60         CALL         putintnl
 61         LOADL        1337
 62         LOADL        -3
 63         LOADL        2
 64         CALL         newobj  
 65         STORE        5[LB]
 66         LOADA        5[LB]
 67         LOADI  
 68         LOADL        0
 69         CALL         ne      
 70         JUMPIF (0)   L15
 71         LOADL        6
 72         CALL         putintnl
 73  L15:   LOADL        7
 74         LOADA        5[LB]
 75         LOADI  
 76         LOADL        0
 77         CALL         fieldref
 78         CALL         add     
 79         LOADA        3[LB]
 80         STOREI 
 81         LOADA        3[LB]
 82         LOADI  
 83         CALL         putintnl
 84         LOADL        -3
 85         LOADL        2
 86         CALL         newobj  
 87         LOADA        5[LB]
 88         LOADI  
 89         LOADL        1
 90         LOAD         -3[ST]
 91         CALL         fieldupd
 92         POP          1
 93         LOADL        8
 94         LOADA        5[LB]
 95         LOADI  
 96         LOADL        1
 97         CALL         fieldref
 98         LOADL        0
 99         LOAD         -3[ST]
100         CALL         fieldupd
101         POP          1
102         LOADA        5[LB]
103         LOADI  
104         LOADL        1
105         CALL         fieldref
106         LOADL        0
107         CALL         fieldref
108         CALL         putintnl
109         LOADL        1337
110         LOADL        4
111         CALL         newarr  
112         STORE        6[LB]
113         LOADA        6[LB]
114         LOADI  
115         CALL         arraylen
116         LOADA        3[LB]
117         STOREI 
118         LOADL        2
119         LOADA        3[LB]
120         LOADI  
121         CALL         mult    
122         LOADL        1
123         CALL         add     
124         CALL         putintnl
125         LOADA        6[LB]
126         LOADI  
127         LOADL        0
128         LOADL        0
129         CALL         arrayupd
130         LOADL        1
131         LOADA        4[LB]
132         STOREI 
133  L16:   LOADA        4[LB]
134         LOADI  
135         LOADA        6[LB]
136         LOADI  
137         CALL         arraylen
138         CALL         lt      
139         JUMPIF (0)   L17
140         LOADA        6[LB]
141         LOADI  
142         LOADA        4[LB]
143         LOADI  
144         LOADA        6[LB]
145         LOADI  
146         LOADA        4[LB]
147         LOADI  
148         LOADL        1
149         CALL         sub     
150         CALL         arrayref
151         LOADA        4[LB]
152         LOADI  
153         CALL         add     
154         CALL         arrayupd
155         LOADA        4[LB]
156         LOADI  
157         LOADL        1
158         CALL         add     
159         LOADA        4[LB]
160         STOREI 
161         POP          0
162         JUMP         L16
163  L17:   LOADA        6[LB]
164         LOADI  
165         LOADL        3
166         CALL         arrayref
167         LOADL        4
168         CALL         add     
169         LOADA        3[LB]
170         STOREI 
171         LOADA        3[LB]
172         LOADI  
173         CALL         putintnl
174         LOADA        5[LB]
175         LOADI  
176         CALLI        L18
177         LOADL        999
178         CALL         putintnl
179         RETURN (0)   1
180  L18:   LOADL        1337
181         LOADL        11
182         STORE        3[LB]
183         LOADA        3[LB]
184         LOADI  
185         CALL         putintnl
186         LOADA        0[OB]
187         LOADA        0[OB]
188         LOADL        1
189         CALL         fieldref
190         LOADL        1
191         LOAD         -3[ST]
192         CALL         fieldupd
193         POP          1
194         LOADL        12
195         LOADA        0[OB]
196         LOADL        0
197         LOAD         -3[ST]
198         CALL         fieldupd
199         POP          1
200         LOADA        0[OB]
201         LOADL        1
202         CALL         fieldref
203         LOADL        1
204         CALL         fieldref
205         LOADL        0
206         CALL         fieldref
207         LOADA        3[LB]
208         STOREI 
209         LOADA        3[LB]
210         LOADI  
211         CALL         putintnl
212         LOADL        4
213         LOADA        0[OB]
214         LOADL        0
215         LOAD         -3[ST]
216         CALL         fieldupd
217         POP          1
218         LOADL        2
219         LOADL        4
220         LOADL        3
221         LOADA        0[OB]
222         CALLI        L19
223         CALL         add     
224         LOADA        3[LB]
225         STOREI 
226         LOADA        3[LB]
227         LOADI  
228         CALL         putintnl
229         LOADL        8
230         LOADL        3
231         LOADA        0[OB]
232         LOADL        1
233         CALL         fieldref
234         CALLI        L21
235         CALL         add     
236         CALL         putintnl
237         LOADL        4
238         LOADA        0[OB]
239         LOADL        0
240         LOAD         -3[ST]
241         CALL         fieldupd
242         POP          1
243         LOADL        5
244         LOADA        0[OB]
245         LOADL        1
246         CALL         fieldref
247         LOADL        0
248         LOAD         -3[ST]
249         CALL         fieldupd
250         POP          1
251         LOADL        2
252         LOADA        0[OB]
253         LOADL        1
254         CALL         fieldref
255         LOADA        0[OB]
256         LOADA        0[OB]
257         CALLI        L20
258         CALL         add     
259         CALL         putintnl
260         RETURN (0)   0
261  L19:   LOADA        0[OB]
262         LOADL        0
263         CALL         fieldref
264         LOADA        -1[LB]
265         LOADI  
266         CALL         add     
267         LOADA        -2[LB]
268         LOADI  
269         CALL         add     
270         RETURN (1)   2
271         RETURN (0)   2
272         RETURN (0)   2
273  L20:   LOADA        -1[LB]
274         LOADI  
275         LOADL        0
276         CALL         fieldref
277         LOADA        -2[LB]
278         LOADI  
279         LOADL        0
280         CALL         fieldref
281         CALL         add     
282         LOADA        0[OB]
283         LOADL        0
284         CALL         fieldref
285         CALL         add     
286         RETURN (1)   2
287         RETURN (0)   2
288         RETURN (0)   2
289  L21:   LOADL        1337
290         LOADL        1
291         STORE        3[LB]
292         LOADA        -1[LB]
293         LOADI  
294         LOADL        1
295         CALL         gt      
296         JUMPIF (0)   L22
297         LOADA        -1[LB]
298         LOADI  
299         LOADA        -1[LB]
300         LOADI  
301         LOADL        1
302         CALL         sub     
303         LOADA        0[OB]
304         CALLI        L21
305         CALL         mult    
306         LOADA        3[LB]
307         STOREI 
308  L22:   LOADA        3[LB]
309         LOADI  
310         RETURN (1)   1
311         RETURN (0)   1
312         RETURN (0)   1
