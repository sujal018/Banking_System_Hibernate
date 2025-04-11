package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.ServletException;

import model.Transaction;
import util.HibernateUtil;
import org.hibernate.Session;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

@WebServlet("/bank/*")
public class BankServlet extends HttpServlet {
    private static final double MIN_BALANCE = 1000;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        String path = req.getPathInfo();

        out.println("<html><head><title>Bank</title></head><body>");

        if (path == null || "/".equals(path)) {
            out.println("<h2>Welcome to Online Banking</h2>");
            out.println("<a href='deposit'>Deposit</a><br>");
            out.println("<a href='withdraw'>Withdraw</a><br>");
            out.println("<a href='balance'>Check Balance</a><br>");
        } else if ("/deposit".equals(path)) {
            out.println("<h2>Deposit Amount</h2>");
            out.println("<form method='post' action='deposit'>");
            out.println("<input type='number' name='amount' step='0.01' required>");
            out.println("<button type='submit'>Deposit</button></form>");
        } else if ("/withdraw".equals(path)) {
            out.println("<h2>Withdraw Amount</h2>");
            out.println("<form method='post' action='withdraw'>");
            out.println("<input type='number' name='amount' step='0.01' required>");
            out.println("<button type='submit'>Withdraw</button></form>");
        } else if ("/balance".equals(path)) {
            double balance = getLatestBalance();
            out.println("<h2>Current Balance: " + balance + "</h2>");
        } else {
            out.println("<h3>Invalid option.</h3>");
        }

        out.println("</body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        try {
            double amount = Double.parseDouble(req.getParameter("amount"));

            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                org.hibernate.Transaction tx = session.beginTransaction();
                double currentBalance = getLatestBalance();

                if ("/deposit".equals(path)) {
                    double newBalance = currentBalance + amount;
                    saveTransaction(session, "Deposit", amount, newBalance);
                    out.println("<h3>Deposited ₹" + amount + ". New Balance: ₹" + newBalance + "</h3>");
                } else if ("/withdraw".equals(path)) {
                    if (currentBalance - amount < MIN_BALANCE) {
                        out.println("<h3>Cannot withdraw. Minimum balance of ₹1000 required.</h3>");
                    } else {
                        double newBalance = currentBalance - amount;
                        saveTransaction(session, "Withdraw", amount, newBalance);
                        out.println("<h3>Withdrawn ₹" + amount + ". New Balance: ₹" + newBalance + "</h3>");
                    }
                }

                tx.commit();
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("<h3>Error: " + e.getMessage() + "</h3>");
        }
    }

    private double getLatestBalance() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction last = session.createQuery("FROM Transaction ORDER BY transactionTime DESC", Transaction.class)
                                      .setMaxResults(1)
                                      .uniqueResult();
            return last != null ? last.getBalance() : 10000;
        }
    }

    private void saveTransaction(Session session, String operation, double amount, double newBalance) {
        Transaction txn = new Transaction();
        txn.setAmount(amount);
        txn.setBalance(newBalance);
        txn.setOperation(operation);
        txn.setTransactionTime(LocalDateTime.now());
        session.persist(txn);
    }
}
