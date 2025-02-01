<!DOCTYPE html>
<html lang="ja">
<head prefix="og: http://ogp.me/ns# article: http://ogp.me/ns/article#">
${head!}
	<meta charset="utf-8">
	<meta name="robots" content="noindex">
	<meta name="viewport" content="width=device-width,initial-scale=1">
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
		<#include "css/markdown.css">

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
		<#if !config.hide!?contains('header') && !hide!?contains('header')>
		<div class="default">
			<div class="content">
				<#if (_PREVIEW!false) == true>
				<a class="title" href="${title_url!?ensure_starts_with("/")}">
				<#else>
				<a class="title" href="${siteurl!}${title_url!?ensure_starts_with("/")}">
				</#if>
				${title!}
				</a>
				<form class="search" method="GET" action="${baseurl}search.html?"
					onsubmit="if(document.getElementById('search-keyword').value.length == 0) { return false; }">
					<input id="search-keyword" type="search" name="keyword" placeholder="検索">
				</form>
			</div>
		</div>
		</#if>

		<#if header?has_content>
		<div class="markdown"><@markdown>${header!}</@markdown></div>
		</#if>
		
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
		<#if footer?has_content>
		<div class="markdown"><@markdown>${footer!}</@markdown></div>
		</#if>

		<#if !config.hide!?contains('footer') && !hide!?contains('footer')>
		<div class="default">
			<div class="content">

				<#-- LEGAL -->
				<#if legal?has_content>
				<div class="markdown legal"><@markdown>${legal!}</@markdown></div>
				</#if>

				<span class="copyright">
					${copyright!}&ensp;
					<#if mailto?has_content><address><a href="mailto:${mailto}"></#if>
					<#if author?has_content>${author}<#else>${mailto!}</#if>
					<#if mailto?has_content></a></address></#if>
				</span>
			</div>
		</div>
		</#if>
	</footer>

	<script>
		var ua = window.navigator.userAgent.toLowerCase();
		if (ua.indexOf("windows") !== -1) {
			document.documentElement.style.setProperty("--is-windows", 1)
		} else if (ua.indexOf("mac os") !== -1) {
			document.documentElement.style.setProperty("--is-mac", 1);
		} else if (ua.indexOf("android") !== -1) {
			document.documentElement.style.setProperty("--is-android", 1);
		} else if (ua.indexOf("iphone") !== -1) {
			document.documentElement.style.setProperty("--is-iphone", 1);
		}

		const sticky_header = document.getElementById("sticky-header");
		if (sticky_header != null) {
			const resizeObserver = new ResizeObserver((entries) => {
				if (sticky_header != null) {
					document.documentElement.style.scrollPaddingTop = (sticky_header.clientHeight - 4) + "px";
				}
			});
			resizeObserver.observe(document.body);
		}

		document.addEventListener("copy", (event) => {
			const selection = document.getSelection();
			if (selection.type == "Range") {
				var s = selection.toString();
				s = s.replaceAll(/ ([・：；]) /g, "$1");
				s = s.replaceAll(/ ([“‘（〔［｛〈《「『【])/g, "$1");
				s = s.replaceAll(/([”’）〕］｝〉》」』】、，]) /g, "$1");
				event.clipboardData.setData("text/plain", s);
				event.preventDefault();
			}
		});
	</script>

	<#if (_PREVIEW!false) == true>
	<script>
		var xhr = new XMLHttpRequest();

		function waitForUpdate() {
			xhr.open("GET", "/wait-for-update?random=" + Math.random(), true);
			xhr.send(null);
		}

		xhr.onload = function (e) {
			if (xhr.status === 205 && location.pathname !== "/" && location.pathname !== "/index.html") {
				location.href = "/";
			} else if (xhr.status === 200 || xhr.status === 205) {
				location.reload();
			} else if (xhr.status === 429) {
				// Too Many Requests
			} else {
				waitForUpdate();
			}
		};
		xhr.ontimeout = function (e) {
			waitForUpdate();
		};
		xhr.onerror = function (e) {
			waitForUpdate();
		};
		window.onbeforeunload = function (e) {
			xhr.abort();
		};

		waitForUpdate();
	</script>
	</#if>

${tail!}
</body>
</html>
