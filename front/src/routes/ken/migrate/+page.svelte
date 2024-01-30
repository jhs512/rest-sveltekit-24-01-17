<script lang="ts">
  import rq from '$lib/rq/rq.svelte';

  async function submitLoginForm(this: HTMLFormElement) {
    const form: HTMLFormElement = this;

    form.username.value = form.username.value.trim();
    form.password.value = form.password.value.trim();

    const { data, error } = await rq.apiEndPoints().POST('/api/v1/wikenMigrate/migrate', {
      body: {
        username: form.username.value,
        password: form.password.value
      }
    });

    if (error) rq.msgError(error.msg);
    else {
      rq.msgAndRedirect(data, undefined, '/');
    }
  }
</script>

<form on:submit|preventDefault={submitLoginForm}>
  <div>기존에 아이디/비번을 사용하셨던 분은 아래의 입력상자에 정보를 넣어주세요.</div>
  <div>
    <div>기존 아이디</div>
    <input
      type="text"
      name="username"
      class="input input-bordered"
      placeholder="기존 아이디(필수 아님)"
    />
  </div>

  <div>
    <div>기존 비밀번호</div>
    <input
      type="password"
      name="password"
      class="input input-bordered"
      placeholder=" 기존비밀번호(필수 아님)"
    />
  </div>

  <div>
    <div>마이그레이트</div>
    <button type="submit" class="btn btn-primary">마이그레이트</button>
  </div>
</form>
