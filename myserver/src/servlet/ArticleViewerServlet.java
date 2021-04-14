package servlet;

import dao.NumberCountDAO;
import dao.ArticleManagerDAO;
import util.ArticleInfo;
import util.Log;

import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

public class ArticleViewerServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Log.Verbose("article viewer servlet");

        String action = request.getParameter("action");
        if (action == null) {
            Log.Error("action is null");
            return;
        }
        String index = request.getParameter("index");
        String order = request.getParameter("order");
        String articleId = request.getParameter("articleId");
        Log.Info("action is " + action);

        ArticleManagerDAO articleManagerDAO = new ArticleManagerDAO();
        NumberCountDAO numberCountDAO = new NumberCountDAO();

        switch (action) {
            case "getnum":
                int articleNum = numberCountDAO.getArticleCount();
                Log.Verbose("article num is " + articleNum);
                response.getWriter().write(String.valueOf(articleNum));
            break;
            case "preview":
                ArticleInfo[] list = articleManagerDAO.getLatestArticles(
                        Integer.parseInt(index), 1, order);
                if (list == null || list.length == 0) {
                    Log.Error("get article failure");
                    break;
                }
                response.getWriter().write(list[0].toJson().replace("\r", "\\r").replace("\n", "\\n"));
            break;
            case "view":
                ArticleInfo info = articleManagerDAO.searchArticle(Integer.parseInt(articleId));
                response.getWriter().write(info.toJson());
            break;
            default:
                Log.Warn("unrecognized action " + action);
                break;
        }
    }
}