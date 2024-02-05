<script lang="ts">
  import { page } from '$app/stores';
  import rq from '$lib/rq/rq.svelte';
  import hotkeys from 'hotkeys-js';
  import ToastUiEditor from '$lib/components/ToastUiEditor.svelte';

  function Post__switchTab() {
    var inactiveTabs = document.querySelectorAll('.toastui-editor-tabs > .tab-item:not(.active)');
    inactiveTabs.forEach((value: Element) => {
      (value as HTMLElement).click();
    });
  }

  rq.effect(() => {
    rq.initAuth();

    hotkeys.filter = function (event) {
      return true;
    };

    hotkeys('ctrl+p,cmd+p', 'postEdit', function (event, handler) {
      Post__switchTab();
      event.preventDefault();
    });

    hotkeys.setScope('postEdit');

    return () => {
      hotkeys.deleteScope('postEdit');
    };
  });

  let toastUiEditor: any | undefined;
  let oldBody: string = '';

  function saveToLocalStorage(id: number, body: string) {
    const key = 'posts_' + id;
    // LocalStorage에서 기존 데이터를 가져옵니다.
    const existingData = localStorage.getItem(key);

    // 기존 데이터가 있으면 JSON으로 파싱하고, 없으면 빈 배열을 사용합니다.
    const posts = existingData ? JSON.parse(existingData) : [];

    // 새 데이터를 배열에 추가합니다.
    posts.push({
      id,
      body: body,
      date: new Date().toISOString()
    });

    // 배열의 크기가 50을 초과하면 가장 오래된 항목(첫 번째 항목)을 제거합니다.
    if (posts.length > 50) {
      posts.shift(); // 배열의 첫 번째 항목을 제거합니다.
    }

    // 변경된 배열을 JSON 문자열로 변환하여 LocalStorage에 저장합니다.
    localStorage.setItem(key, JSON.stringify(posts));
  }

  async function Post__saveBody() {
    const newBody = toastUiEditor.editor.getMarkdown().trim();

    if (oldBody === newBody) {
      return;
    }

    const { data, error } = await rq.apiEndPoints().PUT('/api/v1/posts/{id}/body', {
      params: { path: { id: parseInt($page.params.id) } },
      body: { body: newBody }
    });

    oldBody = newBody;

    saveToLocalStorage(parseInt($page.params.id), newBody);

    if (data) {
      rq.msgInfo('본문이 저장되었습니다.');
    }
  }

  async function load() {
    if (import.meta.env.SSR) throw new Error('CSR ONLY');

    const { data, error } = await rq
      .apiEndPoints()
      .GET('/api/v1/posts/{id}', { params: { path: { id: parseInt($page.params.id) } } });

    if (error) throw error;

    return data!;
  }

  async function submitEditForm(this: HTMLFormElement) {
    const form: HTMLFormElement = this;

    const titleInput = form.elements.namedItem('title') as HTMLInputElement;

    if (titleInput.value.length === 0) {
      rq.msgError('제목을 입력해주세요.');
      titleInput.focus();
      return;
    }

    toastUiEditor.editor.setMarkdown(toastUiEditor.editor.getMarkdown().trim());

    if (toastUiEditor.editor.getMarkdown().trim().length === 0) {
      rq.msgError('내용을 입력해주세요.');
      toastUiEditor.editor.focus();
      return;
    }

    console.log(
      'toastUiEditor.editor.getMarkdown().trim() : ' + toastUiEditor.editor.getMarkdown().trim()
    );

    const { data, error } = await rq.apiEndPoints().PUT('/api/v1/posts/{id}', {
      params: { path: { id: parseInt($page.params.id) } },
      body: {
        title: titleInput.value,
        tagContents: form.tagContents.value
          .split('#')
          .map((tagContent: string) => tagContent.trim())
          .filter((tag: string) => tag.length > 0),
        body: toastUiEditor.editor.getMarkdown().trim(),
        published: form.published.checked,
        listed: form.listed.checked
      }
    });

    if (oldBody !== toastUiEditor.editor.getMarkdown().trim()) {
      saveToLocalStorage(parseInt($page.params.id), toastUiEditor.editor.getMarkdown().trim());
    }

    rq.msgAndRedirect(data, error, '/p/' + $page.params.id);
  }
</script>

<div class="flex-grow flex justify-center">
  <div class="w-full max-w-screen-2xl px-2 my-4">
    <div class="text-center">
      <div class="font-bold text-lg">글 편집</div>
      <div class="mt-3 text-gray-400">
        글의 제목, 내용, 공개 여부, 리스트 여부, 태그를 수정할 수 있습니다. 글 본문만 수정하고
        싶다면, 에디터의 저장 버튼을 눌러주세요.
      </div>
    </div>

    <div class="divider"></div>

    {#await load()}
      <div>loading...</div>
    {:then { data: { item: post } }}
      <form on:submit|preventDefault={submitEditForm} class="grid grid-cols-1 gap-4">
        <!-- svelte-ignore a11y-label-has-associated-control -->
        <label class="form-control">
          <div class="label">
            <span class="label-text">번호</span>
          </div>
          <div class="badge">{post.id}</div>
        </label>

        <!-- svelte-ignore a11y-label-has-associated-control -->
        <label class="form-control">
          <div class="label">
            <span class="label-text">추천</span>
          </div>
          <div class="badge">{post.likesCount.toLocaleString()}</div>
        </label>

        <label class="form-control">
          <div class="label">
            <span class="label-text">공개</span>
          </div>
          <input
            class="toggle"
            type="checkbox"
            name="published"
            value={true}
            checked={post.published}
          />
        </label>

        <label class="form-control">
          <div class="label">
            <span class="label-text">글 목록에서 공개</span>
          </div>
          <input class="toggle" type="checkbox" name="listed" value={true} checked={post.listed} />
        </label>

        <label class="form-control">
          <div class="label">
            <span class="label-text">제목</span>
          </div>
          <input
            class="input input-bordered"
            type="text"
            name="title"
            value={post.title}
            maxlength="150"
          />
        </label>

        <label class="form-control">
          <div class="label">
            <span class="label-text">태그</span>
          </div>
          <input
            class="input input-bordered"
            type="text"
            name="tagContents"
            value={post.tagContents.map((tag) => `#${tag}`).join(' ')}
            maxlength="50"
          />
          <div class="label">
            <span class="label-text-alt"> 구분자는 `#` 입니다. </span>
          </div>
        </label>

        <div class="form-control">
          <div class="label">
            <span class="label-text">내용</span>
          </div>
          <div>
            {#key post.id}
              <ToastUiEditor bind:this={toastUiEditor} body={post.body} saveBody={Post__saveBody} />
            {/key}
          </div>
          <div class="label">
            <span class="label-text-alt">
              에디터의 F 키를 누르면 에디터 풀스크린 모드가 토글 됩니다. 해당 단축키는 Ctrl(Cmd) + Q
              입니다. 기타 다른 단축키는 메인 화면의 공지사항을 참고해주세요.
            </span>
          </div>
        </div>

        <div class="form-control">
          <div class="label">
            <span class="label-text">영상 1</span>
          </div>
          <div>
            <input class="file-input file-input-bordered" type="file" name="video__1" />
          </div>
          <div class="label">
            <span class="label-text-alt">
              mp4 파일만 업로드 가능합니다. 파일 업로드는 5MB 이하만 가능합니다.
            </span>
          </div>
        </div>

        <div class="form-control">
          <div class="label">
            <span class="label-text">영상 2</span>
          </div>
          <div>
            <input class="file-input file-input-bordered" type="file" name="video__2" />
          </div>
          <div class="label">
            <span class="label-text-alt">
              mp4 파일만 업로드 가능합니다. 파일 업로드는 5MB 이하만 가능합니다.
            </span>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
          <button
            class="btn btn-outline"
            type="button"
            onclick={() => window.confirm('편집을 마치시겠습니까?') && history.back()}>취소</button
          >
          <button class="btn btn-primary" type="submit">
            <i class="fa-solid fa-pen-to-square"></i> 저장
          </button>
        </div>
      </form>
    {:catch error}
      {error.msg}
    {/await}
  </div>
</div>
