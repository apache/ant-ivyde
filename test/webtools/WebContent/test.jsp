<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page import="org.apache.commons.lang.builder.EqualsBuilder"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Classpath test page</title>
</head>
<body>
<p>
EqualsBuilder is in classpath: <% out.println(EqualsBuilder.class.getName()); %>
</p>
</body>
</html>
