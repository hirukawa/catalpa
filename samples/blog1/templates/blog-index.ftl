<!DOCTYPE html>
<html lang="ja">
<head prefix="og: http://ogp.me/ns# article: http://ogp.me/ns/article#">
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width,initial-scale=1">
	<meta name="robots" content="noindex">
	<link rel="canonical" href="${url?remove_ending('index.html')}">
	<#if icon?has_content><link rel="icon" href="<#if !icon?starts_with("/")>${baseurl}</#if>${icon}"></#if>
	<title>${title!}</title>
	<meta name="description" content="${(description!)?replace('\n', '')}">
	<#if theme_color?has_content><meta name="theme-color" content="${theme_color}"></#if>

	<!-- OGP -->
	<meta property="og:site_name" content="${blog.title!}">
	<meta property="og:type" content="website">
	<meta property="og:url" content="${siteurl!}">
	<meta property="og:title" content="${title!}">
	<meta property="og:description" content="${(description!)?replace('\n', '')}">
	<#if image?has_content><meta property="og:image" content="${siteurl!}/${image}"></#if>

	<#include "templates/webfont" ignore_missing=true>

	<style><@compress single_line=true>
		<#include "css/system.css">
		<#include "css/color.css">
		<#include "css/main.css">
		<#include "css/blog.css">

		body {
			background-color: #f8f8f8;
		}
		main .content {
			padding: 0;
		}

		<#include "css/custom.css" ignore_missing=true>
		${css!}
	</@compress></style>
</head>
<body>
	<#-- header -->
	<header>
		<div class="default">
			<div class="content">
				<#if (_PREVIEW!false) == true>
				<a class="title" href="/">${title!}</a>
				<#else>
				<a class="title" href="${siteurl!}">${title!}</a>
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
				<a class="blog-category<#if blog.category?? && blog.category.url == category.url> active</#if>" href="${baseurl}${category.url}">${category.name}&nbsp;<span class="label" style="font-feature-settings:'halt'">（${category.posts?size}）</span></a>
			</#list>
			</div>
		</div>
	</header>

	<#-- main -->
	<main>
		<div class="content">

			<#-- カテゴリーページ以外 かつ ページ数が 1 より多い場合にページャーを表示します -->
			<#if !blog.category?? && blog.pages?size gt 1>
			<#-- pager -->
			<div class="blog-pager">
				<#if blog.pages?size gt 1>
				<a class="page-previous" <#if blog.page.previous??>href="${baseurl}${blog.page.previous.url}"</#if>></a>
				<#list blog.pages as page>
					<a class="page-jump <#if page.url == blog.page.url>current</#if>" <#if page.url != blog.page.url>href="${baseurl}${page.url}"</#if>>${page.name}</a>
				</#list>
				<a class="page-next" <#if blog.page.next??>href="${baseurl}${blog.page.next.url}"</#if>></a>
				</#if>
			</div>
			<#else>
			<#-- ページャーを表示しないときは代わりに余白を追加します -->
			<div style="height:0;margin-top:2em;"></div>
			</#if>

			<#-- article -->
			<div class="blog-card-container">
				<#list blog.page.posts as post>
				<a class="blog-card blog-page" href="${baseurl}${post.url}">
					<div class="part1">
						<#if post.thumbnail?has_content>
						<#if post.thumbnail?starts_with("data:")>
						<img src="${post.thumbnail}">
						<#else>
						<img src="${baseurl}${post.thumbnail!}">
						</#if>
						<#else>
						<img>
						</#if>
					</div>
					<div class="part2">
						<h2 class="title">${post.title!}</h2>
						<div class="bottom">
							<#if post.categories?size gt 0>
							<span class="category">${post.categories[0].name}</span>
							</#if>
							<span class="date">${post.date}</span>
						</div>
					</div>
				</a>
				</#list>
				<div class="blog-card blog-page hidden"></div>
				<div class="blog-card blog-page hidden"></div>
				<div class="blog-card blog-page hidden"></div>
				<div class="blog-card blog-page hidden"></div>
			</div>

			<#-- カテゴリーページ以外 かつ ページ数が 1 より多い場合にページャーを表示します -->
			<#if !blog.category?? && blog.pages?size gt 1>
			<#-- pager -->
			<div class="blog-pager">
				<#if blog.pages?size gt 1>
				<a class="page-previous" <#if blog.page.previous??>href="${baseurl}${blog.page.previous.url}"</#if>></a>
				<#list blog.pages as page>
					<a class="page-jump <#if page.url == blog.page.url>current</#if>" <#if page.url != blog.page.url>href="${baseurl}${page.url}"</#if>>${page.name}</a>
				</#list>
				<a class="page-next" <#if blog.page.next??>href="${baseurl}${blog.page.next.url}"</#if>></a>
				</#if>
			</div>
			<#else>
			<#-- ページャーを表示しないときは代わりに余白を追加します -->
			<div style="height:0;margin-bottom:2em"></div>
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
