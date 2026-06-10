import openpyxl
wb=openpyxl.load_workbook(r 学期课表 2 )
ws=wb.active
for row in ws.iter_rows(values_only=True):
    print(row)
