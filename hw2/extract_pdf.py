import PyPDF2

pdf_path = r"c:\Users\Mara\Desktop\seweb\hw2\Semantic Web Homework 2.pdf"
with open(pdf_path, 'rb') as file:
    reader = PyPDF2.PdfReader(file)
    text = ""
    for page in reader.pages:
        text += page.extract_text() + "\n"
    print(text)
