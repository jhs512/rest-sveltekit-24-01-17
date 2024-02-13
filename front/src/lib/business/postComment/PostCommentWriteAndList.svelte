<script lang="ts">
  import type { components } from '$lib/types/api/v1/schema';
  import PostCommentWrite from '$lib/business/postComment/PostCommentWrite.svelte';
  import PostCommentList from '$lib/business/postComment/PostCommentList.svelte';

  const { post } = $props<{ post: components['schemas']['PostWithBodyDto'] }>();
</script>

<h1>댓글</h1>

<div><span class="text-gray-400">{post.title}</span> 에 대한 댓글</div>

<PostCommentWrite {post} />
<PostCommentList {post} />
