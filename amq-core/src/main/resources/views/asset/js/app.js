/**
 * NOTO : 需先引入 JQ 及 riot.js
 */

/**
 * 日期的属性扩展
 * @param fmt
 * @returns {*}
 * @constructor
 */
Date.prototype.Format = function(fmt){
    var o = {
        "M+" : this.getMonth()+1,                 //月份
        "d+" : this.getDate(),                    //日
        "h+" : this.getHours(),                   //小时
        "m+" : this.getMinutes(),                 //分
        "s+" : this.getSeconds(),                 //秒
        "q+" : Math.floor((this.getMonth()+3)/3), //季度
        "S"  : this.getMilliseconds()             //毫秒
    };
    if(/(y+)/.test(fmt)){
        fmt=fmt.replace(RegExp.$1, (this.getFullYear()+"").substr(4 - RegExp.$1.length));
    }
    for(var k in o) {
        if(new RegExp("("+ k +")").test(fmt)) {
            fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));
        }
    }
    return fmt;
};

/**
 * 日期格式化
 * @param time
 * @param fmt
 * @returns {string | void | *}
 */
Date.fmt = function(time,fmt){
    var d = new Date(time);
    fmt = (fmt == "" || fmt ==undefined) ? "yyyy-MM-dd hh:mm:ss":fmt;
    return d.Format(fmt);
};

/**
 * riot 的事件观察 ,要先引入 riot.js
 */
window.eventBus = riot.observable();  // riot 的事件观察
/**
riot路由
 */
var R = route.create();
R("/..", function () {
    viewOf(route);
});

function viewOf(route) {
    eventBus.trigger('open', {view: 'viewbox', uri: "/" + route.uri, params: route.params});
}

//自定义改写route对于路径的解析
route.parser(function (path) {
    var raw = path.split('?'),
        uri = raw[0],
        qs = raw[1],
        params = {}
    if (qs) {
        qs.split('&').forEach(function (v) {
            var c = v.split('=')
            params[c[0]] = c[1]
        });
    }
    //属性回写到route
    route.uri = uri;
    route.params = params;
    return route;
});