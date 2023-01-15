f = open('out2.log','r')

In = f.readlines();
f.close();

values = []

for i in In:
    try:
        values.append(float(i[108:-1]))
    except all and ZeroDivisionError and ValueError and Exception:
        pass

avg = float(0)
for i in values:
    avg = avg + i
avg = avg / len(values)
print(str(avg))