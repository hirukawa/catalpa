<!DOCTYPE html>
<html lang="ja">
<head prefix="og: http://ogp.me/ns# article: http://ogp.me/ns/article#">
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width,initial-scale=1">

	<#-- 2ページ目以降は noindex を設定します。（次ページがあるかどうかで2ページ目以降かどうかを判定できます。）-->
	<#if blog.pager.next??>
	<meta name="robots" content="noindex">
	<#elseif siteurl?has_content>
	<link rel="canonical" href="${siteurl}">
	</#if>

	<link rel="icon" href="${baseurl}favicon.ico">
	<title>${blog.title!}</title>
	<meta name="description" content="${(description!)?replace('\n', '')}">

	<!-- OGP -->
	<meta property="og:site_name" content="${blog.title!}">
	<meta property="og:type" content="website">
	<meta property="og:url" content="${siteurl!}">
	<meta property="og:title" content="${blog.title!}">
	<meta property="og:description" content="${(description!)?replace('\n', '')}">
	<#if blog.post.image?has_content><meta property="og:image" content="${siteurl!}/${blog.post.image}"></#if>

	<style>
		<#include "css/main.css">

		header .container {
			max-width: 960px;
		}
		main {
			max-width: 1200px;
		}
		footer .container {
			max-width: 960px;
		}
		.card-container {
			padding: 0 2px 0 0;
		}
		.card {
			padding: 12px min(16px, 2vw) 16px min(16px, 2vw);
			background-color: #fefefe;
			border-top: 2px dotted #ccc;
			border-left: 2px dotted #ccc;
			border-right: none;
			border-bottom: none;
		}
	</style>
</head>
<body>
	<#-- header -->
	<header>
		<div class="container">
			<#if (_PREVIEW!false) == true>
			<a class="title" href="/">${blog.title!}</a>
			<#else>
			<a class="title" href="${siteurl!}">${blog.title!}</a>
			</#if>
			<div style="width:0px;height:16px"></div>
			<form method="GET" action="${baseurl}search.html?"
				onsubmit="if(document.getElementById('search-keyword').value.length == 0) { return false; }">
				<input id="search-keyword" type="search" name="keyword" placeholder="検索">
			</form>
		</div>
	</header>

	<#-- main -->
	<main>
		<#-- category -->
		<div class="categories">
		<#list blog.categories as category>
			<a class="category" href="${baseurl}${category.url}">${category.name}&nbsp;<span class="label">(${category.posts?size})</span></a>
		</#list>
		</div>

		<#-- article -->
		<div class="card-container">
			<div class="card-erase-horizontal-border"></div>
			<div class="card-erase-vertical-border"></div>

			<#list blog.page.posts as post>
			<a class="card" href="${baseurl}${post.url}">
				<div class="date">${post.date}</div>
				<#if post.thumbnail?has_content && post.thumbnail?starts_with("data:") >
				<img src="${post.thumbnail}">
				<#else>
				<img src="${baseurl}${post.thumbnail!}">
				</#if>
				<h2 class="title">${post.title}</h2>
			</a>
			</#list>
			<div class="card hidden"></div>
			<div class="card hidden"></div>
			<div class="card hidden"></div>
			<div class="card hidden"></div>
			<div class="card hidden"></div>
			<div class="card hidden"></div>
		</div>

		<#-- pager -->
		<#if blog.pager.previous?? || blog.pager.next??>
		<div class="pager">
			<#if blog.pager.previous??>
			<a class="previous" href="${blog.pager.previous.url}">前のページ</a>
			</#if>
			<#if blog.pager.next??>
			<a class="next" href="${blog.pager.next.url}">次のページ</a>
			</#if>
		</div>
		</#if>
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
