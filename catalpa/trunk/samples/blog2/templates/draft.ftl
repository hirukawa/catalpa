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
		<#include "css/markdown.css">
		<#include "css/highlight.css">

		header .container {
			max-width: 992px;
		}
		main {
			max-width: 992px;
		}
		article .header {
			margin-block-start: 3rem;
			margin-block-end: 3rem;
		}
		article .header .date {
			font-size: 1.2rem;
			font-weight: normal;
			line-height: 1.0;
			font-style: italic;
		}
		article .header .category {
			display: inline-block;
			vertical-align: top;
			font-size: 1rem;
			font-weight: normal;
			line-height: 1.0;
			background-color: #eeeeee;
			border-radius: 0.2rem;
			padding: 0.5rem;
			text-decoration: none;
			color: #2573b8;
			margin-block-end: 0.25rem;
		}
		article .header .category:hover {
			color: var(--text-link-hover-color);
		}
		article .header .title {
			margin-block-start: 0;
			margin-block-end: 0;
			font-size: 2rem; /* 16px -> 32px */
			font-weight: bold;
			line-height: 1.2;
		}
		article .header .title::first-letter {
			font-size: 3rem;
		}

		.pager {
			padding: 0 16px;
		}
		.pager .previous, 
		.pager .next {
			padding: 1em 1.5em;
			width: clamp(0px, 360px, 100vw);
		}
		.pager .title {
			overflow: hidden;
			white-space: nowrap;
			text-overflow: ellipsis;
		}

		footer {
			margin: 1px 0 0 0;
		}

		.sns {
			margin: 1rem 0;
			padding: 1em 1em;
			line-height: 1;
			background-color: #f2f2f2;
		}
		.sns .button {
			margin: 1em 0.25em 0 0;
			display: inline-block;
			font-family: "Yu Gothic Medium", "Yu Gothic", 'YuGothic';
			font-weight: bold;
			font-size: 87.5%; /* 16px -> 14px */
			line-height: 1;
			padding: 0.5em 0.75em;
			border-radius: 0.25em;
			color: white;
		}
		.sns .button svg {
			height: 1em;
			width: auto;
			vertical-align: middle;
		}
		.sns .button span {
			vertical-align: middle;
		}
		.sns .button.hatena {
			background-color: #00A4DE;
		}
		.sns .button.hatena:hover {
			background-color: #0091C5;
		}
		.sns .button.facebook {
			background-color: #4267B2;
		}
		.sns .button.facebook:hover {
			background-color: #365899;
		}
		.sns .button.twitter {
			background-color: #1DA1F2;
		}
		.sns .button.twitter:hover {
			background-color: #0C7ABF;
		}

		.categories {
			box-sizing: border-box;
			padding: 0 1px 1px 1px;
			gap: 2px;
			background-color: var(--main-background-color);
		}
		.category {
			background-color: #f2f2f2;
			color: var(--text-link-color);
		}
		.category:visited {
			background-color: #f2f2f2;
			color: var(--text-link-color);
		}
		.category:hover {
			background-color: #f2f2f2;
			color: var(--text-link-hover-color);
		}
		.card {
			width: clamp(0px, 200px, 100vw);
		}
	</style>
</head>
<body>
	<#-- header -->
	<header>
		<div class="container">
			<a class="title" href="/">下書き<span style="font-size:75%">（記事数 ${blog.draft.postCount}）${blog.draft.characterCount} 文字</span></a>
			<div style="width:0px;height:16px"></div>
			<form method="GET" action="${baseurl}search.html?"
				onsubmit="if(document.getElementById('search-keyword').value.length == 0) { return false; }">
				<input id="search-keyword" type="search" name="keyword" placeholder="検索">
			</form>
		</div>
	</header>

	<#-- main -->
	<main>
		<article>
			<div class="header">
				<span class="date">${blog.post.date}</span>
				<#list blog.post.categories as category>
				&thinsp;<a class="category" href="${baseurl}${category.url}">${category.name}</a>
				</#list>
				<h1 class="title">${blog.post.title}</h1>
			</div>
			<div class="content markdown"><@markdown replace_backslash_to_yensign=true use_ruby=true use_catalpa_font=true>${content!}</@markdown></div>

			<div style="margin:1rem;text-align:right;color:#666">
				<#if dateModified??>
				<span style="font-size:93.75%">最終更新日</span>
				<span class="datetime">${dateModified?string["yyyy-MM-dd"]}</span>
				</#if>
			</div>
		</article>

		<#-- pager -->
		<#if blog.pager.previous?? || blog.pager.next??>
		<div class="pager">
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
