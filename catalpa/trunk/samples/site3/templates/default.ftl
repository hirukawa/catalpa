<!DOCTYPE html>
<html lang="ja">
<head prefix="og: http://ogp.me/ns# article: http://ogp.me/ns/article#">
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width,initial-scale=1">
	<#if url?has_content><link rel="canonical" href="${url?remove_ending('index.html')}"></#if>
	<link rel="icon" href="${baseurl}favicon.ico">
	<title>${title!}</title>
	<meta name="description" content="${(description!)?replace('\n', '')}">

	<!-- OGP -->
	<meta property="og:site_name" content="${title!}">
	<meta property="og:type" content="website">
	<meta property="og:url" content="${url!}">
	<meta property="og:title" content="${title!}">
	<meta property="og:description" content="${(description!)?replace('\n', '')}">
	<#if image?has_content><meta property="og:image" content="${siteurl!}/${image}"></#if>

	<style>
		<#include "css/main.css">
		<#include "css/markdown.css">
		<#include "css/highlight.css">
		<#include "css/custom.css">

		header .container {
			max-width: 992px;
		}
		main {
			max-width: 992px;
		}
		footer .container {
			max-width: 992px;
		}
	</style>
</head>
<body>
	<#-- header -->
	<header>
		<div class="container">
			<#if (_PREVIEW!false) == true>
			<a class="title" href="/">${title!}</a>
			<#else>
			<a class="title" href="{siteurl!}">${title!}</a>
			</#if>
			<form method="GET" action="${baseurl}search.html?"
				onsubmit="if(document.getElementById('search-keyword').value.length == 0) { return false; }">
				<input id="search-keyword" type="search" name="keyword" placeholder="検索">
			</form>
		</div>
	</header>

	<#-- main -->
	<main>
		<div class="content markdown"><!--start-search-target--><@markdown replace_backslash_to_yensign=true use_ruby=true use_catalpa_font=true>${content!}</@markdown><!--end-search-target--></div>
	</main>

	<#-- footer -->
	<footer>
		<div class="container">
			<#if mailto?has_content>
			&thinsp;
			${copyright!}&ensp;<address><a href="mailto:${mailto}"><#if author?has_content>${author}<#else>${mailto}</#if></a></address>
			</#if>
		</div>
	</footer>

	<script>
		const main = document.getElementsByTagName("main")[0];
		const resizeObserver = new ResizeObserver((entries) => {
			var fontSize = getComputedStyle(document.documentElement).getPropertyValue("--main-font-size-px");
			document.documentElement.style.setProperty("--main-padding-left-adjust", Math.floor(main.clientWidth % parseInt(fontSize) / 2) + "px");
			document.documentElement.style.setProperty("--main-padding-right-adjust", Math.ceil(main.clientWidth % parseInt(fontSize) / 2) + "px");
		});
		resizeObserver.observe(document.body);
	</script>

	<#if (_PREVIEW!false) == true>
	<script>
		function waitForUpdate() {
			var xhr = new XMLHttpRequest();
			xhr.onload = function (e) {
				if (xhr.readyState === 4) {
					if (xhr.status === 200) {
						location.reload();
					}
					waitForUpdate();
				}
			};
			xhr.onerror = function (e) {
				waitForUpdate();
			};
			xhr.open("GET", "/wait-for-update", true);
			xhr.send(null);
		}
		waitForUpdate();
	</script>
	</#if>
</body>
</html>
