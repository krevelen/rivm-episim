<#-- Freemarker template, optionally handling JSP commands -->
<#-- $Id$ -->
<html class="no-js ui-mobile-rendering" lang="en">
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>EpiSim Mobile</title>
<link href="css/style.css" type="text/css" rel="stylesheet" />
<script data-main="scripts/main" src="scripts/lib/require.js"></script>
</head>
<body>
	<header>asd</header>
	<div>
		<h1>EpiSim Mobile</h1>
		<p>
			<a class="btn btn-primary get-forecast">Get Forecast</a>
		</p>
  <h1>Welcome ${users?first}!</h1>
  <p>Our latest product:
  <a href="${users?first}">${users?first}</a>!
  	</div>
	<div id="container"></div>
</body>
</html>
