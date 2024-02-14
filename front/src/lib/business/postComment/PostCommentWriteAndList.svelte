<script lang="ts">
  import type { components } from '$lib/types/api/v1/schema';

  import rq from '$lib/rq/rq.svelte';

  import PostCommentWrite from '$lib/business/postComment/PostCommentWrite.svelte';
  import PostCommentList from '$lib/business/postComment/PostCommentList.svelte';

  const { post } = $props<{
    post: components['schemas']['PostWithBodyDto'];
  }>();
  let postComments = $state<components['schemas']['PostCommentDto'][]>([]);

  async function loadPostComments() {
    if (import.meta.env.SSR) throw new Error('CSR ONLY');

    const { data } = await rq.apiEndPoints().GET('/api/v1/postComments/{postId}', {
      params: {
        path: {
          postId: post.id
        }
      }
    });

    postComments = data!.data.items;
  }
</script>

<h1>댓글</h1>

<div><span class="text-gray-400">{post.title}</span> 에 대한 댓글</div>

{#await loadPostComments()}
  loading...
{:then}
  <PostCommentWrite {post} {postComments} />
  <PostCommentList {post} {postComments} />
{:catch error}
  {error.msg}
{/await}
