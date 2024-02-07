<script lang="ts">
  import { page } from '$app/stores';
  import rq from '$lib/rq/rq.svelte';
  import hotkeys from 'hotkeys-js';
  import ToastUiEditor from '$lib/components/ToastUiEditor.svelte';
  import { prettyDateTime } from '$lib/utils';
  import type { components } from '$lib/types/api/v1/schema';

  let toastUiEditor = $state<any | undefined>();
  let writeCommentToastUiEditor = $state<any | undefined>();
  let editCommentToastUiEditor = $state<any | undefined>();

  let tempPostCommentId = $state(0);
  let post = $state() as components['schemas']['PostWithBodyDto'];
  let forEditPostComment = $state<components['schemas']['PostCommentDto']>();

  async function loadPost() {
    if (import.meta.env.SSR) throw new Error('CSR ONLY');

    const { data, error } = await rq
      .apiEndPoints()
      .GET('/api/v1/posts/{id}', { params: { path: { id: parseInt($page.params.id) } } });

    if (error) throw error;

    Post__lastModified = data!.data.item.modifyDate;
    post = data!.data.item;

    Post__loadLatestBody();

    return data!;
  }

  let Post__lastModified = '';

  async function Post__loadLatestBody() {
    // 에디터 자체가 아직 로드가 안되었다면 1초 후에 재시도
    if (toastUiEditor === undefined) {
      setTimeout(Post__loadLatestBody, 1000);
      return;
    }

    // 브라우저 자체가 숨겨져있거나, 뷰어가 풀스크린이 아니라면 5초 후에 재시도
    if (document.hidden || !toastUiEditor.isFullScreen) {
      setTimeout(Post__loadLatestBody, 5000);
      return;
    }

    const postId = $page.params.id;

    const { data, error } = await rq.apiEndPoints().GET('/api/v1/posts/{id}/body', {
      params: { path: { id: parseInt(postId) }, query: { lastModifyDate: Post__lastModified } }
    });

    if (data) {
      rq.msgInfo(data.msg);

      toastUiEditor.editor.setMarkdown(data.data.body);

      Post__lastModified = data.data.modifyDate;
    }

    setTimeout(Post__loadLatestBody, 5000);
  }

  let postComments = $state<components['schemas']['PostCommentDto'][]>([]);
  let postSubComments = $state<components['schemas']['PostCommentDto'][]>([]);

  async function loadPostComments() {
    if (import.meta.env.SSR) throw new Error('CSR ONLY');

    const { data, error } = await rq.apiEndPoints().GET('/api/v1/postComments/{postId}', {
      params: { path: { postId: parseInt($page.params.id) } }
    });

    if (error) throw error;

    postComments = data!.data.items;

    return data!;
  }

  function showPostSubComments(postCommentId: number) {
    loadPostSubComments(postCommentId);

    const modal = document.querySelector(`#sub_comments_modal_1`) as HTMLDialogElement;

    modal.showModal();
  }

  async function loadPostSubComments(postCommentId: number) {
    if (import.meta.env.SSR) throw new Error('CSR ONLY');

    const { data, error } = await rq
      .apiEndPoints()
      .GET('/api/v1/postComments/{postId}/{postCommentId}/children', {
        params: { path: { postId: parseInt($page.params.id), postCommentId: postCommentId } }
      });

    if (error) throw error;

    postSubComments = data!.data.items;

    return data!;
  }

  async function confirmAndDeletePostComment(
    postComment: components['schemas']['PostCommentDto'],
    callback: (data: components['schemas']['RsDataEmpty']) => void
  ) {
    if (!confirm('삭제하시겠습니까?')) return;

    const { data, error } = await rq
      .apiEndPoints()
      .DELETE('/api/v1/postComments/{postId}/{postCommentId}', {
        params: {
          path: { postId: parseInt($page.params.id), postCommentId: postComment.id }
        }
      });

    if (error) throw error;

    callback(data!);
  }

  async function submitEditCommentForm(id: number) {
    const toastUiEditor = editCommentToastUiEditor;

    toastUiEditor.editor.setMarkdown(toastUiEditor.editor.getMarkdown().trim());

    if (toastUiEditor.editor.getMarkdown().trim().length === 0) {
      rq.msgError('내용을 입력해주세요.');
      toastUiEditor.editor.focus();
      return;
    }

    const { data, error } = await rq
      .apiEndPoints()
      .PUT('/api/v1/postComments/{postId}/{postCommentId}', {
        params: {
          path: { postId: parseInt($page.params.id), postCommentId: id }
        },
        body: {
          body: toastUiEditor.editor.getMarkdown()
        }
      });

    rq.msgInfo(data!.msg);

    console.log('forEditPostComment! : ' + JSON.stringify(forEditPostComment!));
    console.log('data!.data.item : ' + JSON.stringify(data!.data.item));

    Object.assign(forEditPostComment!, data!.data.item);

    const modal = document.querySelector(`#post_comment_edit_modal_1`) as HTMLDialogElement;
    modal.close();
  }

  async function makeTempPostComment() {
    const { data } = await rq.apiEndPoints().POST('/api/v1/postComments/{postId}/temp', {
      params: { path: { postId: parseInt($page.params.id) } }
    });

    tempPostCommentId = data!.data.item.id;
    setTimeout(() => {
      toastUiEditor.editor.focus();
    }, 100);
  }

  async function submitWriteCommentForm() {
    writeCommentToastUiEditor.editor.setMarkdown(
      writeCommentToastUiEditor.editor.getMarkdown().trim()
    );

    if (writeCommentToastUiEditor.editor.getMarkdown().trim().length === 0) {
      rq.msgError('내용을 입력해주세요.');
      writeCommentToastUiEditor.editor.focus();
      return;
    }

    const { data, error } = await rq
      .apiEndPoints()
      .PUT('/api/v1/postComments/{postId}/{postCommentId}', {
        params: { path: { postId: parseInt($page.params.id), postCommentId: tempPostCommentId } },
        body: {
          body: writeCommentToastUiEditor.editor.getMarkdown()
        }
      });

    writeCommentToastUiEditor.editor.setMarkdown('');
    tempPostCommentId = 0;

    rq.msgInfo(data!.msg);

    // postComments 맨 앞에 넣고 싶어
    postComments.unshift(data!.data.item);
    post.commentsCount++;

    (window.document.querySelector('#post_comment_write_modal_1') as HTMLDialogElement).close();
  }

  function Post__switchTab() {
    var inactiveTabs = document.querySelectorAll('.toastui-editor-tabs > .tab-item:not(.active)');
    inactiveTabs.forEach((value: Element) => {
      (value as HTMLElement).click();
    });
  }

  rq.effect(() => {
    hotkeys.filter = function (event) {
      return true;
    };

    hotkeys('ctrl+p,cmd+p', 'postDetail', function (event, handler) {
      Post__switchTab();
      event.preventDefault();
    });

    hotkeys('ctrl+d,cmd+d', 'postDetail', function (event, handler) {
      toastUiEditor.toggleFullScreen();
      event.preventDefault();
    });

    hotkeys.setScope('postDetail');

    return () => {
      hotkeys.deleteScope('postDetail');
    };
  });

  function showWriteCommentForm() {
    makeTempPostComment();
    const modal = window.document.querySelector('#post_comment_write_modal_1') as HTMLDialogElement;
    modal.showModal();
  }

  function showComments() {
    loadPostComments();
    const modal = window.document.querySelector('#post_comments_modal_1') as HTMLDialogElement;
    modal.showModal();
  }
</script>

<div class="flex-grow flex justify-center items-center">
  <div class="w-full px-2 mt-4">
    {#await loadPost()}
      <div>loading...</div>
    {:then { }}
      <div class="card bg-base-100 shadow">
        <div class="card-body">
          <div class="detail grid grid-cols-[repeat(auto-fit,minmax(100px,1fr))] gap-3">
            <div class="form-control">
              <!-- svelte-ignore a11y-label-has-associated-control -->
              <label class="label">
                <span class="label-text">번호</span>
              </label>
              <div>{post.id}</div>
            </div>

            <div class="form-control">
              <!-- svelte-ignore a11y-label-has-associated-control -->
              <label class="label">
                <span class="label-text">작성일</span>
              </label>
              <div>{prettyDateTime(post.createDate)}</div>
            </div>

            <div class="form-control">
              <!-- svelte-ignore a11y-label-has-associated-control -->
              <label class="label">
                <span class="label-text">작성자</span>
              </label>
              <div>{post.authorName}</div>
            </div>

            <div class="form-control">
              <!-- svelte-ignore a11y-label-has-associated-control -->
              <label class="label">
                <span class="label-text">공개여부</span>
              </label>
              {#if post.published}
                <i class="fa-regular fa-eye"></i>
              {:else}
                <i class="fa-regular fa-eye-slash"></i>
              {/if}
            </div>

            <div class="form-control col-span-full">
              <!-- svelte-ignore a11y-label-has-associated-control -->
              <label class="label">
                <span class="label-text">제목</span>
              </label>
              <div>{post.title}</div>
            </div>

            <div class="flex gap-2">
              {#if post.actorCanDelete}
                <button
                  class="btn btn-outline"
                  onclick={() => rq.confirmAndDeletePost(post, '/p/list')}>삭제</button
                >
              {/if}

              {#if post.actorCanEdit}
                <a class="btn btn-outline" href="/p/{post.id}/edit">수정</a>
              {/if}
            </div>

            <div class="form-control col-span-full">
              <!-- svelte-ignore a11y-label-has-associated-control -->
              <label class="label">
                <span class="label-text">내용</span>
              </label>

              {#key post.id}
                <ToastUiEditor bind:this={toastUiEditor} body={post.body} viewer={true}>
                  <div slot="beforeContent">
                    <!-- svelte-ignore a11y-click-events-have-key-events -->
                    <!-- svelte-ignore a11y-no-noninteractive-element-interactions -->
                    <h1>
                      제목 : {post.title},
                      <div class="tooltip tooltip-right" data-tip="Ctrl + d, Cmd + d">
                        <button class="btn btn-sm" on:click={() => toastUiEditor.toggleFullScreen()}
                          >전체화면</button
                        >
                      </div>
                    </h1>
                  </div>
                </ToastUiEditor>
              {/key}
            </div>
          </div>
        </div>
      </div>

      <div>새 댓글기능 시작</div>

      <div class="flex flex-col">
        <button class="btn btn-link" onclick={showWriteCommentForm}>댓글 작성</button>
        {#if post.commentsCount > 0}
          <button class="btn btn-link" onclick={showComments}>{post.commentsCount} 개의 댓글</button
          >
        {:else}
          <div class="text-center">댓글이 없습니다.</div>
        {/if}
      </div>

      <dialog id="post_comment_write_modal_1" class="modal">
        <div class="modal-box max-w-7xl">
          <h3 class="font-bold text-lg">댓글 작성</h3>

          <form on:submit|preventDefault={submitWriteCommentForm}>
            {#if tempPostCommentId > 0}
              <div>
                <div>내용</div>
                {#key tempPostCommentId}
                  <ToastUiEditor
                    bind:this={writeCommentToastUiEditor}
                    body={''}
                    saveBody={() => submitWriteCommentForm()}
                  />
                {/key}
              </div>

              <div>
                <button class="btn btn-outline" type="submit">댓글작성</button>
              </div>
            {/if}
          </form>
        </div>
        <form method="dialog" class="modal-backdrop">
          <button>close</button>
        </form>
      </dialog>

      <dialog id="post_comment_edit_modal_1" class="modal">
        <div class="modal-box max-w-7xl">
          <h3 class="font-bold text-lg">댓글 수정</h3>

          {#if forEditPostComment !== undefined}
            <form on:submit|preventDefault={() => submitEditCommentForm(forEditPostComment!.id)}>
              <div>
                <div>내용</div>
                {#key forEditPostComment.id}
                  <ToastUiEditor
                    bind:this={editCommentToastUiEditor}
                    body={forEditPostComment.body}
                    saveBody={() => submitEditCommentForm(forEditPostComment!.id)}
                  />
                {/key}
              </div>

              <div>
                <button class="btn btn-outline" type="submit">저장</button>
              </div>
            </form>
          {/if}
        </div>
        <form method="dialog" class="modal-backdrop">
          <button>close</button>
        </form>
      </dialog>

      <dialog id="post_comments_modal_1" class="modal">
        <div class="modal-box max-w-7xl">
          <h3 class="font-bold text-lg">댓글 목록</h3>

          <ul class="flex flex-col gap-5">
            {#each postComments as postComment}
              <li class="card bg-base-100 shadow">
                <div class="card-body">
                  <div>번호 : {postComment.id}</div>
                  <div>작성 : {prettyDateTime(postComment.createDate)}</div>
                  <div>수정 : {prettyDateTime(postComment.modifyDate)}</div>
                  <div>작성자 : {postComment.authorName}</div>
                  <div>
                    <img
                      src={postComment.authorProfileImgUrl}
                      width="30"
                      class="rounded-full"
                      alt=""
                    />
                  </div>
                  <div>
                    {#key `${postComment.id}__${postComment.modifyDate}`}
                      <ToastUiEditor body={postComment.body} viewer={true} />
                    {/key}
                  </div>
                  <div>
                    <div>
                      {#if postComment.actorCanDelete}
                        <button
                          class="btn btn-outline"
                          onclick={() =>
                            confirmAndDeletePostComment(postComment, (data) => {
                              rq.msgInfo(data.msg);
                              postComments.splice(postComments.indexOf(postComment), 1);
                              post.commentsCount--;
                            })}>삭제</button
                        >
                      {/if}

                      {#if postComment.actorCanEdit}
                        <button
                          class="btn btn-outline"
                          onclick={() => {
                          forEditPostComment = postComment;
                          const modal = document.querySelector(`#post_comment_edit_modal_1`) as HTMLDialogElement;
                          modal.showModal();
                        }}
                          >수정</button
                        >
                      {/if}

                      {#if postComment.actorCanReply}
                        <button class="btn btn-outline">답글</button>
                      {/if}

                      {#if postComment.childrenCount > 0}
                        <div>
                          <button class="btn" onclick={() => showPostSubComments(postComment.id)}>
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
        </div>

        <form method="dialog" class="modal-backdrop">
          <button>close</button>
        </form>
      </dialog>
    {:catch error}
      {error.msg}
    {/await}
  </div>
</div>
