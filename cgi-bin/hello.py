import os

print("Content-Type: text/html\r\n\r\n")
print("<h1>CGI Test</h1>")
print("<pre>")
print("METHOD  : " + os.environ.get("REQUEST_METHOD", "?"))
print("QUERY   : " + os.environ.get("QUERY_STRING", "?"))
print("LENGTH  : " + os.environ.get("CONTENT_LENGTH", "?"))
print("</pre>")