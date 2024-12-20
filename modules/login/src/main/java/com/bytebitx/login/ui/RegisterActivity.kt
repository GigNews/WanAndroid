package com.bytebitx.login.ui

import android.content.Intent
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bytebitx.base.base.BaseActivity
import com.bytebitx.base.constants.Constants
import com.bytebitx.base.constants.RouterPath
import com.bytebitx.base.ext.Resource
import com.bytebitx.base.ext.observe
import com.bytebitx.base.ext.showToast
import com.bytebitx.base.util.AppUtil
import com.bytebitx.login.R
import com.bytebitx.login.bean.LoginData
import com.bytebitx.login.databinding.ActivityRegisterBinding
import com.bytebitx.login.viewmodel.RegisterLoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 *  author: wangyb
 *  date: 2021/5/27 2:47 下午
 *  description: todo
 */
@Route(path = RouterPath.LoginRegister.PAGE_REGISTER)
@AndroidEntryPoint
class RegisterActivity : BaseActivity<ActivityRegisterBinding>(), View.OnClickListener {

    @Inject
    lateinit var registerLoginViewModel: RegisterLoginViewModel

    override fun initView() {
        binding.actionBar.apply {
            tvTitle.text = getString(R.string.register)
            setSupportActionBar(binding.actionBar.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        binding.btnRegister.setOnClickListener(this)
        binding.tvSignIn.setOnClickListener(this)
    }

    override fun initObserver() {
        observe(registerLoginViewModel.registerLoginLiveData, ::handleRegister)
    }

    override fun initData() {
    }

    private fun handleRegister(resource: Resource<LoginData>) {
        when (resource) {
            is Resource.Loading -> {

            }
            is Resource.Error -> {
                showToast(resource.exception.toString())
            }
            is Resource.Success -> {
                AppUtil.isLogin = true
                ARouter.getInstance().build(intent.getStringExtra(Constants.ROUTER_PATH))
                    .navigation()
//                LiveDataBus.get().with(BusKey.LOGIN_SUCCESS).value = Any()
                finish()
            }
        }
    }

    override fun onClick(v: View?) {
        v ?: return
        when (v.id) {
            binding.btnRegister.id -> {
                if (binding.etUsername.text.toString().isEmpty()) {
                    showToast(getString(R.string.username_not_empty))
                    return
                }
                if (binding.etPassword.text.toString().isEmpty()) {
                    showToast(getString(R.string.password_not_empty))
                    return
                }
                if (binding.etPassword2.text.toString().isEmpty()) {
                    showToast(getString(R.string.confirm_password_not_empty))
                    return
                }
                if (binding.etPassword.text.toString() != binding.etPassword2.text.toString()) {
                    showToast(getString(R.string.password_cannot_match))
                    return
                }
                registerLoginViewModel.register(
                    binding.etUsername.text.toString(),
                    binding.etPassword.text.toString(),
                    binding.etPassword2.text.toString()
                )
            }
            binding.tvSignIn.id -> {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            else -> {

            }
        }
    }
}