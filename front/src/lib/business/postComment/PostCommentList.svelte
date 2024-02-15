<script lang="ts">
  import { page } from '$app/stores';
  import rq from '$lib/rq/rq.svelte';
  import type { components } from '$lib/types/api/v1/schema';
  import { prettyDateTime } from '$lib/utils';

  const { post, postComments, deletePostComment } = $props<{
    post: components['schemas']['PostWithBodyDto'];
    postComments: components['schemas']['PostCommentDto'][];
    deletePostComment: (postComment: components['schemas']['PostCommentDto']) => void;
  }>();

  async function confirmAndDeletePostComment(postComment: components['schemas']['PostCommentDto']) {
    if (!confirm('삭제하시겠습니까?')) return;

    const { data, error } = await rq
      .apiEndPoints()
      .DELETE('/api/v1/postComments/{postId}/{postCommentId}', {
        params: {
          path: { postId: parseInt($page.params.id), postCommentId: postComment.id }
        }
      });

    if (error) throw error;

    deletePostComment(postComment);

    rq.msgInfo(data.msg);
  }
</script>

<h2>댓글 목록</h2>

<ul class="grid gap-4">
  {#each postComments as postComment (postComment.id)}
    <li class="card bg-base-100 shadow">
      <div class="card-body">
        <div>번호 : {postComment.id}</div>
        <div>작성 : {prettyDateTime(postComment.createDate)}</div>
        <div>수정 : {prettyDateTime(postComment.modifyDate)}</div>
        <div>작성자 : {postComment.authorName}</div>
        <div>
          <img src={postComment.authorProfileImgUrl} width="30" class="rounded-full" alt="" />
        </div>
        <div>
          {postComment.body}
        </div>
        <div>
          <div>
            {#if postComment.actorCanDelete}
              <button
                class="btn btn-outline"
                onclick={() => confirmAndDeletePostComment(postComment)}>삭제</button
              >
            {/if}

            {#if postComment.actorCanEdit}
              <button class="btn btn-outline">수정</button>
            {/if}

            {#if postComment.actorCanReply}
              <button class="btn btn-outline">답글</button>
            {/if}

            {#if postComment.childrenCount > 0}
              <div>
                <button class="btn">
                  총 {postComment.childrenCount}개의 답글
                </button>
              </div>
            {/if}
          </div>
        </div>
      </div>
    </li>
  {/each}
</ul>
