{
  template: `<div>
    <a-form-model
      class="user-layout-login"
      ref="formLogin"
      :model="form"
      @submit="handleSubmit"
    >
      <a-form-model-item>
        <a-input
          size="large"
          type="text"
          placeholder="user code"
          v-model="form.accounts"
          v-decorator="validate1"
        >
          <a-icon slot="prefix" type="user" :style="{ color: color }"/>
        </a-input>
      </a-form-model-item>
      <a-form-model-item>
        <a-input
          size="large"
          type="password"
          autocomplete="false"
          placeholder="password"
          v-model="form.password"
          v-decorator="validate2"
        >
          <a-icon slot="prefix" type="lock" :style="{ color: color }"/>
        </a-input>
      </a-form-model-item>
      <a-form-model-item style="margin-top:24px">
        <a-button
          size="large"
          type="primary"
          htmlType="submit"
          class="login-button"
          :loading="loading"
          :disabled="disabled"
        >
          Sign in
        </a-button>
      </a-form-model-item>
    </a-form-model>
  </div>`,
  name: 'UserCenterLogin',
  props: {
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      color: 'rgba(0,0,0,.25)',
      validate1: [
            'accounts',
            { rules: [{ required: true, message: '请输入工号' }], validateTrigger: 'change' }
          ],
      validate2: [
            'password',
            { rules: [{ required: true, message: '请输入密码' }], validateTrigger: 'blur' }
          ],
      form: {
        account_name: '',
        password: ''
      }
    }
  },
  created () {},
  computed: {
    disabled () {
      return !this.form.accounts || !this.form.password
    }
  },
  methods: {
    handleUsernameOrEmail (rule, value, callback) {
      callback()
    },
    handleSubmit (e) {
      e.preventDefault()
      const data = { ...this.form }
      data.ps_salt = Math.random().toString(36).substring(3, 7)
      data.password = this.$md5(this.$md5(data.password) + data.ps_salt)
      this.$emit('finished', data)
    }
  }
}