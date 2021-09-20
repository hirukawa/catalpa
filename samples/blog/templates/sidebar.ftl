
<ul class="categories">
<#list blog.categories as category>
	<li style="float:left"><a href="${baseurl}${category.url}">${category.name}&thinsp;<span class="label">(${category.posts?size})</span></a></li>
</#list>
</ul>

<div class="recent-posts-header">最新記事</div>
<ul class="recent-posts">
<#list blog.posts as post>
	<#if post?index == 10>
		<#break>
	</#if>
	<li>
		<a href="${baseurl}${post.url}">
			<#if post.thumbnail?has_content && post.thumbnail?starts_with("data:") >
				<img src="${post.thumbnail}">
			<#else>
				<img src="${baseurl}${post.thumbnail!}">
			</#if>
			<div>
				<div class="datetime font-bold">${post.date}</div>
				${post.title}
			</div>
		</a>
	</li>
</#list>
</ul>

<a href="${baseurl}sample.html">▶ 固定ページ</a>
