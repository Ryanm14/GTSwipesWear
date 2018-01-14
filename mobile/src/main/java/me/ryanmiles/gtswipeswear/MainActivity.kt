package me.ryanmiles.gtswipeswear

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Window
import android.webkit.*
import android.widget.Toast


class MainActivity : AppCompatActivity() {

    lateinit var webView: WebView
    val LOGINURL = "https://login.gatech.edu/cas/login?service=https%3a%2f%2fmealplan.gatech.edu%2fAuth%2fLogin%3freturnUrl%3d%2fDashboard%2fBuzzCardBalance"
    var first = true
    val dataList = ArrayList<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_PROGRESS)
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        setContentView(R.layout.activity_main)
        Toast.makeText(this, "Log in to your Georgia Tech Account!", Toast.LENGTH_LONG * 2).show()

        webView = findViewById<WebView>(R.id.webView)
        CookieManager.getInstance().acceptThirdPartyCookies(webView)
        webView.clearCache(true)
        webView.clearHistory()
        webView.loadUrl(LOGINURL)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (!url.contains("dev.m") || url.contains("cas")) {
                    view.loadUrl(url)
                } else {
                    // run "mockup"-specific code
                    val cookies = CookieManager.getInstance().getCookie(url)
                    //                    String[] splitParams = url.split("\\?")[1].split("&");
                    val splitParams = cookies.split("=".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    //                    String sessionName = splitParams[0].split("=")[1];
                    //                    String sessionId = splitParams[1].split("=")[1];
                    val sessionName = splitParams[0]
                    val sessionId = splitParams[1]
                    Log.d(this.toString(), "Session Name: $sessionName  Session Id: $sessionId")
                }
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                Log.d(this.toString(), "OnPageStarted url: $url")
                if (!first && url == LOGINURL) {
                    Toast.makeText(baseContext, "Duo Confirm pls", Toast.LENGTH_LONG).show()
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (url == "https://mealplan.gatech.edu/Error") {
                    view.loadUrl("https://mealplan.gatech.edu/Dashboard/BuzzCardBalance")

                } else if (url == "https://mealplan.gatech.edu/Dashboard/BuzzCardBalance") {
                    view.evaluateJavascript(
                            "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",
                            ValueCallback<String>
                            { html ->
                                var d = html.substringAfterLast("pre-wrap;\\\">")
                                d = d.substringBefore("\\")
                                if (d != "\"") {
                                    dataList.add(d)
                                    view.loadUrl("https://mealplan.gatech.edu/Dashboard/DiningPointsBalance")
                                }

                            })
                } else if (url == "https://mealplan.gatech.edu/Dashboard/DiningPointsBalance") {
                    view.evaluateJavascript(
                            "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",
                            ValueCallback<String>
                            { html ->
                                var d = html.substringAfterLast("pre-wrap;\\\">")
                                d = d.substringBefore("\\")
                                if (d !== "\"") {
                                    dataList.add(d)
                                    view.loadUrl("https://mealplan.gatech.edu/Dashboard/BlockPlanBalance")
                                }
                            })
                } else if (url == "https://mealplan.gatech.edu/Dashboard/BlockPlanBalance") {
                    view.evaluateJavascript(
                            "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",
                            ValueCallback<String>
                            { html ->
                                var d = html.substringAfter("{\\\"BlockCount\\\":")
                                d = d.substringBefore(",")
                                if (d !== "\"") {
                                    dataList.add(d)
                                    print(dataList)
                                }
                                // code here
                            })
                } else {
                    Log.d(this.toString(), "OnPageFinished url: $url")
                    val pass = "pass"
                    view.evaluateJavascript("javascript:document.getElementById('username').value = 'rmiles30';javascript:document.getElementById('password').value = '$pass';document.getElementsByName('submit')[0].click();", ValueCallback<String> { })
                    first = false
                }
            }


            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                val alertDialogBuilder = AlertDialog.Builder(
                        applicationContext)

                alertDialogBuilder.setTitle("Refresh page")
                alertDialogBuilder
                        .setMessage("Login page has failed to load. Would you like to try again?")
                        .setCancelable(false)
                        .setPositiveButton("Refresh", DialogInterface.OnClickListener { dialog, id ->
                            // if this button is clicked, just close
                            // the dialog box and do nothing
                            webView.reload()
                        })

                        .setNegativeButton("Cancel",
                                DialogInterface.OnClickListener { dialog, id ->
                                    // if this button is clicked, just close
                                    // the dialog box and do nothing
                                    dialog.cancel()
                                })
                // create alert dialog
                val alertDialog = alertDialogBuilder.create()
                alertDialog.show()

            }
        }


        webView.settings.javaScriptEnabled = true

        setProgressBarIndeterminateVisibility(true)
        setProgressBarVisibility(true)


        val activity = this
        webView.webChromeClient =
                object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, progress: Int) {
                        // Activities and WebViews measure progress with different scales.
                        // The progress meter will automatically disappear when we reach 100%
                        activity.setProgress(progress * 100)
                    }
                }
    }


}
