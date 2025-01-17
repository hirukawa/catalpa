<!DOCTYPE html>
<html lang="ja">
<head prefix="og: http://ogp.me/ns# article: http://ogp.me/ns/article#">
${head!}
	<meta charset="utf-8">
	<#if robots?has_content><meta name="robots" content="${robots}"></#if>
	<meta name="viewport" content="width=device-width,initial-scale=1">
	<link rel="canonical" href="${url?remove_ending('index.html')}">
	<#if icon?has_content><link rel="icon" href="<#if !icon?starts_with("/")>${baseurl}</#if>${icon}"></#if>
	<title>${title!}</title>
	<meta name="description" content="${(description!)?replace('\n', '')}">
	<#if theme_color?has_content><meta name="theme-color" content="${theme_color}"></#if>

	<!-- OGP -->
	<meta property="og:site_name" content="${blog.title!}">
	<meta property="og:type" content="article">
	<meta property="og:url" content="${siteurl!}/${blog.post.url}">
	<meta property="og:title" content="${title!}">
	<meta property="og:description" content="${(description!)?replace('\n', '')}">
	<#if blog.post.image?has_content>
	<meta property="og:image" content="${siteurl!}/${blog.post.image}">
	<#elseif image?has_content>
	<meta property="og:image" content="${siteurl!}/${image}">
	</#if>

	<!-- X (Twitter) card -->
	<meta name="twitter:card" content="summary_large_image" />

	<#include "templates/webfont" ignore_missing=true>

	<style><@compress single_line=true>
		<#include "css/system.css">
		<#include "css/color.css">
		<#include "css/main.css">
		<#include "css/blog.css">
		<#include "css/markdown.css">
		<#include "css/highlight.css">

		.blog-pager .previous, 
		.blog-pager .next {
			width: clamp(0px, 360px, 100vw);
		}
		.blog-categories {
			padding: 0 1px 1px 1px;
			gap: 2px;
			background-color: var(--main-background-color);
		}
		.blog-category {
			background-color: #f2f2f2;
			color: var(--text-link-color);
		}
		.blog-category:visited {
			background-color: #f2f2f2;
			color: var(--text-link-color);
		}
		.blog-category:hover {
			background-color: #f2f2f2;
			color: var(--text-link-hover-color);
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
				<#if config.title?has_content>
				${config.title}
				<#else>
				${title!}
				</#if>
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
	</header>

	<#-- main -->
	<main>
		<div class="content">
			<article class="post">
				<div class="blog-header">
					<span class="date">${blog.post.date}&ensp;</span>
					<#list blog.post.categories as category>
					<a class="category" href="${baseurl}${category.url}">${category.name}</a>
					</#list>
					<h1 class="title">${blog.post.title}</h1>
				</div>
				<div class="markdown"><!--start-search-target--><@markdown>${content!}</@markdown><!--end-search-target--></div>

				<div style="margin:1rem;text-align:right;color:#666">
					<#if contentLastModified??>
					<span style="font-size:93.75%">最終更新日</span>
					<span class="datetime">${contentLastModified?string["yyyy-MM-dd"]}</span>
					</#if>
				</div>
			</article>
		</div>

		<#-- SNS -->
		<#if !config.hide!?contains('sns_all') && !hide!?contains('sns_all')>
		<div class="content" style="padding:0">
			<div style="margin:1rem 0;padding:1em;background-color:#f2f2f2">
				<div style="margin-block-end:1em;line-height:1">この記事を共有しませんか？</div>
				<div style="display:flex;flex-wrap:wrap;align-items:center;gap:0.5em">
					<#-- SNS はてなブックマーク -->
					<#if !config.hide!?contains('sns_hatena') && !hide!?contains('sns_hatena')>
					<a class="sns button hatena"
						href="https://b.hatena.ne.jp/add?mode=confirm&url=${url?remove_ending('index.html')}&title=${title!}" target="_blank"
						alt="はてなブックマーク"
						title="はてなブックマークに追加する"
						style="font-family:var(--font-family-sans-serif);font-weight:500;font-size:14px;line-height:1;margin:0;padding:6px 8px;border-radius:4px;color:white;background-color:#00A4DE">
						<svg width="18px" height="14px" viewBox="0 0 1800 1400" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" style="vertical-align:top;fill-rule:evenodd;clip-rule:evenodd;stroke-linejoin:round;stroke-miterlimit:2;">
							<g transform="matrix(34.1982,0,0,34.1982,-638.92,-838.889)">
								<path d="M50.076,46.458C48.309,44.572 45.859,43.469 43.276,43.398C45.429,42.948 47.398,41.864 48.928,40.284C50.173,38.721 50.802,36.753 50.692,34.758C50.721,33.057 50.31,31.376 49.5,29.88C48.682,28.464 47.485,27.305 46.044,26.532C44.57,25.755 42.976,25.231 41.328,24.984C38.12,24.615 34.89,24.471 31.662,24.552L20.448,24.552L20.448,65.452L32,65.452C35.355,65.52 38.71,65.364 42.044,64.984C43.837,64.739 45.576,64.198 47.192,63.384C48.859,62.52 50.232,61.179 51.134,59.532C52.084,57.757 52.562,55.767 52.52,53.754C52.66,51.101 51.786,48.491 50.076,46.458ZM30.816,33.606L33.21,33.606C35.982,33.606 37.842,33.918 38.79,34.542C39.8,35.296 40.341,36.528 40.212,37.782C40.334,39.029 39.741,40.244 38.682,40.914C37.674,41.526 35.782,41.814 33.03,41.814L30.816,41.814L30.816,33.606ZM40.316,57.06C39.216,57.726 37.346,58.05 34.716,58.05L30.816,58.05L30.816,49.14L34.884,49.14C37.584,49.14 39.456,49.482 40.446,50.166C41.53,51.029 42.101,52.388 41.958,53.766C42.119,55.1 41.465,56.408 40.3,57.078L40.316,57.06Z" style="fill:white;fill-rule:nonzero;"/>
							</g>
							<g transform="matrix(34.1982,0,0,34.1982,-638.92,-838.889)">
								<path d="M64.368,55.1C61.524,55.1 59.184,57.44 59.184,60.284C59.184,63.128 61.524,65.468 64.368,65.468C67.212,65.468 69.552,63.128 69.552,60.284C69.552,57.44 67.212,55.1 64.368,55.1Z" style="fill:white;fill-rule:nonzero;"/>
							</g>
							<g transform="matrix(34.1982,0,0,34.1982,-638.92,-838.889)">
								<rect x="59.868" y="24.552" width="9" height="27.274" style="fill:white;"/>
							</g>
						</svg>
						ブックマーク
					</a>
					</#if>
					<#-- SNS Facebook -->
					<#if !config.hide!?contains('sns_facebook') && !hide!?contains('sns_facebook')>
					<a class="sns button facebook"
						href="https://www.facebook.com/share.php?u=${url?remove_ending('index.html')}" target="_blank"
						alt="Facebookシェア"
						title="Facebookでシェアする"
						style="font-family:var(--font-family-sans-serif);font-weight:500;font-size:14px;line-height:1;margin:0;padding:6px 8px;border-radius:4px;color:white;background-color:#1877F2">
						<svg width="18px" height="14px" viewBox="0 0 1800 1400" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" style="vertical-align:top;fill-rule:evenodd;clip-rule:evenodd;stroke-linejoin:round;stroke-miterlimit:2;">
							<g transform="matrix(1.37554,0,0,1.37554,195.722,-0.302579)">
								<path d="M1024,512.22C1024,229.45 794.77,0.22 512,0.22C229.23,0.22 0,229.45 0,512.22C0,767.774 187.231,979.59 432,1018L432,660.22L302,660.22L302,512.22L432,512.22L432,399.42C432,271.1 508.438,200.22 625.39,200.22C681.407,200.22 740,210.22 740,210.22L740,336.22L675.438,336.22C611.835,336.22 592,375.687 592,416.177L592,512.22L734,512.22L711.3,660.22L592,660.22L592,1018C836.769,979.59 1024,767.774 1024,512.22Z" style="fill:white;fill-rule:nonzero;"/>
							</g>
						</svg>
						シェア
					</a>
					</#if>
					<#-- SNS X (Twitter) -->
					<#if !config.hide!?contains('sns_x') && !hide!?contains('sns_x')>
					<a class="sns button x"
						href="https://x.com/intent/post?url=${url?remove_ending('index.html')}&text=${title!}" target="_blank"
						alt="X (Twitter)"
						title="Xにポストする"
						style="font-family:var(--font-family-sans-serif);font-weight:500;font-size:14px;line-height:1;margin:0;padding:6px 8px;border-radius:4px;color:white;background-color:#000000">
						<svg width="18px" height="14px" viewBox="0 0 1800 1400" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" style="vertical-align:top;fill-rule:evenodd;clip-rule:evenodd;stroke-linejoin:round;stroke-miterlimit:2;">
							<path d="M1030.26,592.5l509.713,-592.5l-120.785,0l-442.584,514.459l-353.49,-514.459l-407.71,0l534.547,777.954l-534.547,621.327l120.793,0l467.38,-543.287l373.311,543.287l407.71,0l-554.367,-806.781l0.029,0Zm-165.442,192.308l-54.16,-77.467l-430.938,-616.41l185.53,0l347.772,497.463l54.161,77.466l452.062,646.626l-185.531,0l-368.896,-527.649l0,-0.029Z" style="fill:white;fill-rule:nonzero;"/>
						</svg>
						ポスト
					</a>
					</#if>
				</div>
			</div>
			</#if>

			<#-- 次の記事・前の記事 -->
			<#if blog.post.previous?? || blog.post.next??>
			<div class="blog-pager">
				<#-- 次の記事 -->
				<#if blog.post.next??>
				<a class="post-next" href="${baseurl}${blog.post.next.url}">
					<div class="part1">
						<#if blog.post.next.thumbnail?has_content>
						<#if blog.post.next.thumbnail?starts_with("data:")>
						<img src="${blog.post.next.thumbnail}">
						<#else>
						<img src="${baseurl}${blog.post.next.thumbnail!}">
						</#if>
						<#else>
						<img>
						</#if>
					</div>
					<div class="part2">
						<h2 class="title">${blog.post.next.title!}</h2>
						<div class="bottom">
							<span class="date">${blog.post.next.date}</span>
						</div>
					</div>
				</a>
				</#if>
			    <#-- 前の記事 -->
				<#if blog.post.previous??>
				<a class="post-previous" href="${baseurl}${blog.post.previous.url}">
					<div class="part1">
						<#if blog.post.previous.thumbnail?has_content>
						<#if blog.post.previous.thumbnail?starts_with("data:")>
						<img src="${blog.post.previous.thumbnail}">
						<#else>
						<img src="${baseurl}${blog.post.previous.thumbnail!}">
						</#if>
						<#else>
						<img>
						</#if>
					</div>
					<div class="part2">
						<h2 class="title">${blog.post.previous.title!}</h2>
						<div class="bottom">
							<span class="date">${blog.post.previous.date}</span>
						</div>
					</div>
				</a>
				</#if>
			</div>
			</#if>

			<#-- カテゴリー -->
			<div class="blog-categories">
			<#list blog.categories as category>
				<a class="blog-category" href="${baseurl}${category.url}">${category.name}&nbsp;<span class="label" style="font-feature-settings:'halt'">（${category.posts?size}）</span></a>
			</#list>
			</div>

			<#-- 関連記事 -->
			<div class="blog-related-container">
				<#list blog.post.related as post>
					<#if post?index == 12>
						<#break>
					</#if>
					<a class="blog-related-post" href="${baseurl}${post.url}">
						<div class="part1">
							<#if post.thumbnail?has_content>
							<#if post.thumbnail?starts_with("data:") >
							<img src="${post.thumbnail!}">
							<#else>
							<img src="${baseurl}${post.thumbnail!}">
							</#if>
							<#else>
							<img>
							</#if>
						</div>
						<div class="part2">
							<h2 class="title">${post.title}</h2>
							<div class="bottom">
								<span class="date">${post.date}</span>
							</div>
						</div>
					</a>
				</#list>
				<a class="blog-related-post hidden"></a>
				<a class="blog-related-post hidden"></a>
				<a class="blog-related-post hidden"></a>
				<a class="blog-related-post hidden"></a>
			</div>
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

				<#-- COPYRIGHT -->
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
	</script>

	<#if (_PREVIEW!false) == true>
	<script>
		function waitForUpdate() {
			fetch("/wait-for-update?random=" + Math.random()).then(response => {
				if (response.status === 205 && location.pathname !== "/" && location.pathname !== "/index.html") {
					location.href = "/";
				} else if (response.ok) {
					location.reload();
				} else {
					waitForUpdate();
				}
			}).catch(error => {
				waitForUpdate();
			});
		}
		waitForUpdate();
	</script>
	</#if>

${tail!}
</body>
</html>
