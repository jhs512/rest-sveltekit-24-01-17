<script lang="ts">
  import type { components } from '$lib/types/api/v1/schema';

  const { post, postComments } = $props<{
    post: components['schemas']['PostWithBodyDto'];
    postComments: components['schemas']['PostCommentDto'][];
  }>();
</script>

<h2>댓글 목록</h2>

<ul>
  {#each postComments as postComment (postComment.id)}
    <li>
      <div>{postComment.body}</div>
      <div>{postComment.createDate}</div>
    </li>
  {/each}
</ul>
