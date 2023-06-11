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
          placeholder="Account name"
          v-model="form.account_name"
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
            'username',
            { rules: [{ required: true, message: '请输入帐户名' }], validateTrigger: 'change' }
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
      return !this.form.account_name || !this.form.password
    }
  },
  methods: {
    handleUsernameOrEmail (rule, value, callback) {
      callback()
    },
    handleSubmit (e) {
      e.preventDefault()
      const data = { ...this.form }
      data.password = this.$sha1(data.password).toLowerCase()
      this.$emit('finished', data)
    }
  }
}