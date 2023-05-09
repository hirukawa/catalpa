<!DOCTYPE html>
<html lang="ja">
<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width,initial-scale=1">
	<link rel="icon" href="${baseurl}favicon.ico">
	<title>下書き</title>
	<meta name="description" content="${(description!)?replace('\n', '')}">
	<style>
		<#include "css/main.css">
		<#include "css/blog.css">
		<#include "css/markdown.css">
		<#include "css/highlight.css">
		${css!}
	</style>
</head>
<body>
	<#-- header -->
	<header>
		<div class="default">
			<div class="content">
				<a class="title" href="/">下書き<span style="font-size:75%">（記事数 ${blog.draft.postCount}）${blog.draft.characterCount} 文字</span></a>
				<form class="search" method="GET" action="${baseurl}search.html?"
					onsubmit="if(document.getElementById('search-keyword').value.length == 0) { return false; }">
					<input id="search-keyword" type="search" name="keyword" placeholder="検索">
				</form>
			</div>
		</div>
	</header>

	<#-- main -->
	<main>
		<div class="content">
			<article>
				<div class="blog-header">
					<span class="date">${blog.post.date}</span>
					<#list blog.post.categories as category>
					&thinsp;<a class="blog-category" href="${baseurl}${category.url}">${category.name}</a>
					</#list>
					<h1 class="title">${blog.post.title}</h1>
				</div>
				<div class="markdown"><!--start-search-target--><@markdown replace_backslash_to_yensign=true use_ruby=true use_catalpa_font=true>${content!}</@markdown><!--end-search-target--></div>

				<div style="margin:1rem;text-align:right;color:#666">
					<#if dateModified??>
					<span style="font-size:93.75%">最終更新日</span>
					<span class="datetime">${dateModified?string["yyyy-MM-dd"]}</span>
					</#if>
				</div>
			</article>
		</div>

		<div class="content" style="padding:0">
			<#-- pager -->
			<#if blog.pager.previous?? || blog.pager.next??>
			<div class="blog-pager">
				<#if blog.pager.previous??>
				<a class="previous grow" href="${blog.pager.previous.url}">
					<div>
						前の記事
						&nbsp;
						<span style="">${blog.pager.previous.date}</span>
					</div>
					<div class="title">${blog.pager.previous.title}</div>
				</a>
				</#if>
				<#if blog.pager.next??>
				<a class="next grow" href="${blog.pager.next.url}">
					<div>
						次の記事
						&nbsp;
						<span style="">${blog.pager.next.date}</span>
					</div>
					<div class="title">${blog.pager.next.title}</div>
				</a>
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
		var ua = window.navigator.userAgent.toLowerCase();
		if(ua.indexOf("windows") !== -1) {
			document.documentElement.style.setProperty("--is-windows", 1)
		} else if(ua.indexOf("mac os") !== -1) {
			document.documentElement.style.setProperty("--is-mac", 1);
		} else if(ua.indexOf("android") !== -1) {
			document.documentElement.style.setProperty("--is-android", 1);
		} else if(ua.indexOf("iphone") !== -1) {
			document.documentElement.style.setProperty("--is-iphone", 1);
		}
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
