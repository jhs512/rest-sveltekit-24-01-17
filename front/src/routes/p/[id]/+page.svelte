<script lang="ts">
  import { page } from '$app/stores';
  import rq from '$lib/rq/rq.svelte';

  import PostDetail from '$lib/business/post/PostDetail.svelte';
  import PostCommentWriteAndList from '$lib/business/postComment/PostCommentWriteAndList.svelte';

  async function loadPost() {
    if (import.meta.env.SSR) throw new Error('CSR ONLY');

    const { data, error } = await rq
      .apiEndPoints()
      .GET('/api/v1/posts/{id}', { params: { path: { id: parseInt($page.params.id) } } });

    if (error) throw error;

    return data!;
  }
</script>

<div class="flex-grow flex justify-center items-center">
  <div class="w-full px-2 mt-4">
    {#await loadPost()}
      <div>loading...</div>
    {:then { data: { item: post } }}
      <PostDetail {post} />
      <PostCommentWriteAndList {post} />
    {/await}
  </div>
</div>
