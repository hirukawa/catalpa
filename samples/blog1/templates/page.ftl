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
		<#include "css/blog.css">

		main .content {
			margin-block-start: 1em;
			margin-block-end: 1em;
			max-width: 1200px;
		}
		.blog-card-container {
			padding: 0 2px 0 0;
		}
		.blog-card {
			padding: 12px min(16px, 2vw) 16px min(16px, 2vw);
			background-color: #fefefe;
			border-top: 2px dotted #ccc;
			border-left: 2px dotted #ccc;
			border-right: none;
			border-bottom: none;
		}
		${css!}
	</style>
</head>
<body>
	<#-- header -->
	<header>
		<div class="default">
			<div class="content">
				<#if (_PREVIEW!false) == true>
				<a class="title" href="/">${blog.title!}</a>
				<#else>
				<a class="title" href="${siteurl!}">${blog.title!}</a>
				</#if>
				<form class="search" method="GET" action="${baseurl}search.html?"
					onsubmit="if(document.getElementById('search-keyword').value.length == 0) { return false; }">
					<input id="search-keyword" type="search" name="keyword" placeholder="検索">
				</form>
			</div>
		</div>
		<#-- category -->
		<div class="blog-categories-wrapper">
			<div class="content blog-categories">
			<#list blog.categories as category>
				<a class="blog-category" href="${baseurl}${category.url}">${category.name}&nbsp;<span class="label">(${category.posts?size})</span></a>
			</#list>
			</div>
		</div>
	</header>

	<#-- main -->
	<main>
		<div class="content">
			<#-- article -->
			<div class="blog-card-container">
				<div class="blog-card-erase-horizontal-border"></div>
				<div class="blog-card-erase-vertical-border"></div>

				<#list blog.page.posts as post>
				<a class="blog-card" href="${baseurl}${post.url}">
					<div class="date">${post.date}</div>
					<#if post.thumbnail?has_content && post.thumbnail?starts_with("data:") >
					<img src="${post.thumbnail!}">
					<#else>
					<img src="${baseurl}${post.thumbnail!}">
					</#if>
					<h2 class="title">${post.title}</h2>
				</a>
				</#list>
				<div class="blog-card hidden"></div>
				<div class="blog-card hidden"></div>
				<div class="blog-card hidden"></div>
				<div class="blog-card hidden"></div>
				<div class="blog-card hidden"></div>
				<div class="blog-card hidden"></div>
			</div>

			<#-- pager -->
			<#if blog.pager.previous?? || blog.pager.next??>
			<div class="blog-pager">
				<#if blog.pager.previous??>
				<a class="previous" href="${blog.pager.previous.url}">前のページ</a>
				</#if>
				<#if blog.pager.next??>
				<a class="next" href="${blog.pager.next.url}">次のページ</a>
				</#if>
			</div>
			</#if>
		</div>
	</main>

	<#-- footer -->
	<footer>
		<div class="default">
			<div class="content">
				<span class="copyright" style="margin-inline-end:auto">
					${copyright!}&ensp;
					<#if mailto?has_content><address><a href="mailto:${mailto}"></#if>
					<#if author?has_content>${author}<#else>${mailto!}</#if>
					<#if mailto?has_content></a></address></#if>
				</span>
			</div>
		</div>
	</footer>

	<script>
		const content = document.getElementsByClassName("content")[0];
		const sticky_header = document.getElementById("sticky-header");
		if(content != null || sticky_header != null) {
			const resizeObserver = new ResizeObserver((entries) => {
				if(content != null) {
					var font_size = getComputedStyle(document.documentElement).getPropertyValue("--content-font-size-px");
					document.documentElement.style.setProperty("--content-padding-left-adjust", Math.floor(content.clientWidth % parseInt(font_size) / 2) + "px");
					document.documentElement.style.setProperty("--content-padding-right-adjust", Math.ceil(content.clientWidth % parseInt(font_size) / 2) + "px");
				}
				if(sticky_header != null) {
					document.documentElement.style.scrollPaddingTop = sticky_header.clientHeight + "px";
				}
			});
			resizeObserver.observe(document.body);
		}
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
