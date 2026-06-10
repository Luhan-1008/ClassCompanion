package com.example.myapplication.ui.screen

import android.os.Bundle
import android.view.View
import android.widget.Button
import com.journeyapps.barcodescanner.CaptureActivity

/**
 * 自定义二维码扫描Activity
 * 固定竖屏方向，显示扫描框和取消按钮
 */
class CustomCaptureActivity : CaptureActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 强制竖屏显示
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        // 设置自定义布局（如果ZXing支持）
        // 注意：ZXing的CaptureActivity可能不支持直接设置布局
        // 所以我们在onResume中设置取消按钮

    }
    
    override fun onResume() {
        super.onResume()
        // 延迟一下确保布局已经完成
        window.decorView.post {
            setupCancelButton()
        }
    }
    
    private fun setupCancelButton() {
        // 尝试查找ZXing的取消按钮
        var cancelButton: Button? = null
        
        // 尝试通过资源名称查找
        val zxingCancelId = resources.getIdentifier("zxing_cancel_button", "id", "com.google.zxing.client.android")
        if (zxingCancelId != 0) {
            cancelButton = findViewById(zxingCancelId)
        }
        
        if (cancelButton == null) {
            val localCancelId = resources.getIdentifier("zxing_cancel_button", "id", packageName)
            if (localCancelId != 0) {
                cancelButton = findViewById(localCancelId)
            }
        }
        
        if (cancelButton != null) {
            // 如果找到ZXing的按钮，使用它
            cancelButton.setOnClickListener {
                cancelScan()
            }
            cancelButton.visibility = View.VISIBLE
            cancelButton.text = "取消扫描"
        } else {
            // 如果找不到，添加一个覆盖在顶部的取消按钮
            addTopCancelButton()
        }
    }
    
    private fun addTopCancelButton() {
        // 获取content view
        val contentView = findViewById<View>(android.R.id.content) as? android.view.ViewGroup ?: return
        
        // 检查是否已经添加过
        val existingButton = contentView.findViewById<Button>(android.R.id.button1)
        if (existingButton != null) {
            // 如果已存在，确保点击事件正确
            existingButton.setOnClickListener {
                cancelScan()
            }
            return
        }
        
        // 创建一个覆盖在顶部的FrameLayout来放置取消按钮
        val overlayLayout = android.widget.FrameLayout(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            // 不拦截触摸事件，让扫描视图正常工作
            isClickable = false
            isFocusable = false
            // 确保在顶层
            elevation = 10f
        }
        
        // 创建取消按钮
        val cancelButton = Button(this).apply {
            id = android.R.id.button1
            text = "取消扫描"
            textSize = 16f
            setPadding(24, 12, 24, 12)
            setBackgroundColor(android.graphics.Color.parseColor("#80000000")) // 半透明黑色背景
            setTextColor(android.graphics.Color.WHITE)
            
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // 放在顶部中央
                gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
                topMargin = (50 * resources.displayMetrics.density).toInt() // 转换为像素
            }
            
            // 确保按钮可点击
            isClickable = true
            isFocusable = true
            
            setOnClickListener {
                // 直接调用finish，确保能返回
                cancelScan()
            }
        }
        
        overlayLayout.addView(cancelButton)
        
        // 将overlay添加到content view
        contentView.addView(overlayLayout)
    }
    
    /**
     * 取消扫描并返回
     */
    private fun cancelScan() {
        // 暂停扫描
        try {
            val barcodeViewId = resources.getIdentifier("zxing_barcode_scanner", "id", "com.google.zxing.client.android")
            if (barcodeViewId != 0) {
                val barcodeView = findViewById<com.journeyapps.barcodescanner.DecoratedBarcodeView>(barcodeViewId)
                barcodeView?.pause()
            }
        } catch (e: Exception) {
            // 忽略错误
        }
        
        // 设置取消结果码并关闭Activity
        setResult(android.app.Activity.RESULT_CANCELED)
        finish()
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        cancelScan()
    }
}

