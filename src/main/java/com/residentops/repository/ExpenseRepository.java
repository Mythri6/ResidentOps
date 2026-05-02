package com.residentops.repository;
import com.residentops.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByOrderByCreatedAtDesc();
    @Query("SELECT SUM(e.amount) FROM Expense e")
    Double getTotalExpenses();
}
