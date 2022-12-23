package com.group.libraryapp.service.book

import com.group.libraryapp.domain.book.Book
import com.group.libraryapp.domain.book.BookRepository
import com.group.libraryapp.domain.user.User
import com.group.libraryapp.domain.user.UserRepository
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistory
import com.group.libraryapp.domain.user.loanhistory.UserLoanHistoryRepository
import com.group.libraryapp.dto.book.request.BookLoanRequest
import com.group.libraryapp.dto.book.request.BookRequest
import com.group.libraryapp.dto.book.request.BookReturnRequest
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BookServiceTest @Autowired constructor(
    private val bookRepository: BookRepository,
    private val bookService: BookService,
    private val userRepository: UserRepository,
    private val userLoanHistoryRepository: UserLoanHistoryRepository,
) {

    @AfterEach
    fun clean() {
        bookRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("책 등록이 정상적으로 동작한다.")
    fun saveBookTest() {
        // given
        val request = BookRequest("돈으로 살 수 없는 것", "COMPUTER")

        // when
        bookService.saveBook(request)

        // then
        val books = bookRepository.findAll()
        assertThat(books).hasSize(1)
        assertThat(books.first().name).isEqualTo("돈으로 살 수 없는 것")
        assertThat(books.first().type).isEqualTo("COMPUTER")
    }

    @Test
    @DisplayName("책 대출이 정상적으로 동작한다.")
    fun loanBookTestSuccess() {
        // given
        bookRepository.save(Book.fixture())
        val savedUser = userRepository.save(User("soono", null))
        val request = BookLoanRequest("soono", "운영체제")

        // when
        bookService.loanBook(request)

        // then
        val results = userLoanHistoryRepository.findAll()
        assertThat(results).hasSize(1)
        assertThat(results.first().bookName).isEqualTo("운영체제")
        assertThat(results.first().user.id).isEqualTo(savedUser.id)
        assertThat(results.first().isReturn).isFalse
    }

    @Test
    @DisplayName("책이 이미 대출되어 있다면, 신규 대출이 실패한다.")
    fun loanBookTestFail() {
        // given
        bookRepository.save(Book.fixture())
        val savedUser = userRepository.save(User("soono", null))
        userLoanHistoryRepository.save(UserLoanHistory(savedUser, "운영체제", false))
        val request = BookLoanRequest("soono", "운영체제")

        // when & then
        assertThrows<IllegalArgumentException> {
            bookService.loanBook(request)
        }.apply {
            assertThat(message).isEqualTo("진작 대출되어 있는 책입니다.")
        }
    }

    @Test
    @DisplayName("책 반납이 정상 동작한다.")
    fun returnBookTest() {
        // given
        bookRepository.save(Book.fixture())
        val savedUser = userRepository.save(User("soono", null))
        userLoanHistoryRepository.save(UserLoanHistory(savedUser, "운영체제", false))
        val request = BookReturnRequest("soono", "운영체제")

        // when
        bookService.returnBook(request)

        // then
        val results = userLoanHistoryRepository.findAll()
        assertThat(results).hasSize(1)
        assertThat(results.first().isReturn).isTrue
    }
}