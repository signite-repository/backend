use crate::models::User;
use sqlx::{MySqlPool, Row};
use std::error::Error;

pub async fn find_user_by_username(
    pool: &MySqlPool,
    username: &str,
) -> Result<Option<User>, Box<dyn Error>> {
    let user = sqlx::query_as::<_, User>(
        "SELECT id, username, email, password, full_name, bio, avatar_url, 
         phone_number, location, website, enabled, email_verified, 
         two_factor_enabled, created_at, updated_at 
         FROM users WHERE username = ?"
    )
    .bind(username)
    .fetch_optional(pool)
    .await?;

    if let Some(mut user) = user {
        // Fetch roles
        user.roles = fetch_user_roles(pool, &user.id).await?;
        Ok(Some(user))
    } else {
        Ok(None)
    }
}

pub async fn find_user_by_email(
    pool: &MySqlPool,
    email: &str,
) -> Result<Option<User>, Box<dyn Error>> {
    let user = sqlx::query_as::<_, User>(
        "SELECT id, username, email, password, full_name, bio, avatar_url, 
         phone_number, location, website, enabled, email_verified, 
         two_factor_enabled, created_at, updated_at 
         FROM users WHERE email = ?"
    )
    .bind(email)
    .fetch_optional(pool)
    .await?;

    if let Some(mut user) = user {
        // Fetch roles
        user.roles = fetch_user_roles(pool, &user.id).await?;
        Ok(Some(user))
    } else {
        Ok(None)
    }
}

pub async fn user_exists(
    pool: &MySqlPool,
    username: &str,
    email: &str,
) -> Result<bool, Box<dyn Error>> {
    let count: i64 = sqlx::query_scalar(
        "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?"
    )
    .bind(username)
    .bind(email)
    .fetch_one(pool)
    .await?;

    Ok(count > 0)
}

pub async fn create_user(
    pool: &MySqlPool,
    user: &User,
) -> Result<(), Box<dyn Error>> {
    let mut tx = pool.begin().await?;

    // Insert user
    sqlx::query(
        "INSERT INTO users (id, username, email, password, full_name, bio, 
         avatar_url, phone_number, location, website, enabled, 
         email_verified, two_factor_enabled, created_at) 
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    )
    .bind(&user.id)
    .bind(&user.username)
    .bind(&user.email)
    .bind(&user.password)
    .bind(&user.full_name)
    .bind(&user.bio)
    .bind(&user.avatar_url)
    .bind(&user.phone_number)
    .bind(&user.location)
    .bind(&user.website)
    .bind(user.enabled)
    .bind(user.email_verified)
    .bind(user.two_factor_enabled)
    .bind(user.created_at)
    .execute(&mut *tx)
    .await?;

    // Insert roles
    for role in &user.roles {
        sqlx::query("INSERT INTO user_roles (user_id, role) VALUES (?, ?)")
            .bind(&user.id)
            .bind(role)
            .execute(&mut *tx)
            .await?;
    }

    tx.commit().await?;
    Ok(())
}

pub async fn update_user_profile(
    pool: &MySqlPool,
    username: &str,
    full_name: Option<String>,
    bio: Option<String>,
    phone_number: Option<String>,
    location: Option<String>,
    website: Option<String>,
) -> Result<(), Box<dyn Error>> {
    sqlx::query(
        "UPDATE users SET full_name = ?, bio = ?, phone_number = ?, 
         location = ?, website = ?, updated_at = NOW() 
         WHERE username = ?"
    )
    .bind(full_name)
    .bind(bio)
    .bind(phone_number)
    .bind(location)
    .bind(website)
    .bind(username)
    .execute(pool)
    .await?;

    Ok(())
}

async fn fetch_user_roles(
    pool: &MySqlPool,
    user_id: &str,
) -> Result<Vec<String>, Box<dyn Error>> {
    let roles = sqlx::query("SELECT role FROM user_roles WHERE user_id = ?")
        .bind(user_id)
        .fetch_all(pool)
        .await?
        .into_iter()
        .map(|row| row.get::<String, _>("role"))
        .collect();

    Ok(roles)
}