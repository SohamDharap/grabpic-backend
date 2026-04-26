# GrabPic Backend API Documentation

## Overview
This document provides comprehensive API documentation for the GrabPic backend system, including authentication, event management, photo upload, and face search functionality.

## Base URL
```
http://localhost:8080
```

## Authentication
The API uses JWT (JSON Web Token) based authentication. All protected endpoints require a valid JWT token in the Authorization header.

### Authentication Flow
1. **Request OTP**: Send email to receive OTP
2. **Verify OTP**: Submit OTP to receive JWT token
3. **Use JWT Token**: Include token in Authorization header for protected endpoints

### Authorization Header
```
Authorization: Bearer <jwt_token>
```

## API Endpoints

### 1. Authentication

#### 1.1 Request OTP
**Endpoint**: `POST /api/auth/request-otp`

**Description**: Request OTP for email authentication

**Request Body**:
```json
{
    "email": "photographer@grabpic.com"
}
```

**Response**:
```json
{
    "message": "OTP sent successfully",
    "expiresIn": 600
}
```

**Status Codes**:
- `200`: OTP sent successfully
- `400`: Invalid email format
- `404`: User not found
- `429`: Too many OTP requests

#### 1.2 Verify OTP & Get JWT
**Endpoint**: `POST /api/auth/verify-otp`

**Description**: Verify OTP and receive JWT token

**Request Body**:
```json
{
    "email": "photographer@grabpic.com",
    "otp": "123456"
}
```

**Response**:
```json
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "email": "photographer@grabpic.com",
    "role": "PHOTOGRAPHER"
}
```

**Status Codes**:
- `200`: OTP verified successfully
- `400`: Invalid OTP
- `401`: OTP expired or invalid
- `404`: User not found

### 2. Admin Management (Admin Only)

#### 2.1 Create Photographer
**Endpoint**: `POST /api/admin/photographers`

**Description**: Create a new photographer account (Admin only)

**Authorization**: Required (Admin role)

**Request Body**:
```json
{
    "firstname": "John",
    "lastname": "Doe",
    "email": "newphotographer@grabpic.com",
    "phoneNumber": "9876543210",
    "gender": "MALE",
    "age": 25
}
```

**Response**:
```json
{
    "id": 1,
    "firstname": "John",
    "lastname": "Doe",
    "email": "newphotographer@grabpic.com",
    "phoneNumber": "9876543210",
    "gender": "MALE",
    "age": 25,
    "role": "PHOTOGRAPHER",
    "isActive": true,
    "createdAt": "2026-04-26T18:00:00",
    "updatedAt": "2026-04-26T18:00:00"
}
```

#### 2.2 Get All Photographers
**Endpoint**: `GET /api/admin/photographers`

**Description**: Get list of all photographers (Admin only)

**Authorization**: Required (Admin role)

**Response**:
```json
[
    {
        "id": 1,
        "firstname": "John",
        "lastname": "Doe",
        "email": "newphotographer@grabpic.com",
        "phoneNumber": "9876543210",
        "gender": "MALE",
        "age": 25,
        "role": "PHOTOGRAPHER",
        "isActive": true
    }
]
```

### 3. Event Management

#### 3.1 Create Event
**Endpoint**: `POST /api/events`

**Description**: Create a new event

**Authorization**: Required (Photographer/Admin role)

**Request Body**:
```json
{
    "name": "Birthday Party",
    "venue": "Community Hall",
    "eventDate": "2026-05-15T18:00:00",
    "ownerName": "John Doe",
    "ownerContact": "9876543210"
}
```

**Response**:
```json
{
    "id": 1,
    "name": "Birthday Party",
    "venue": "Community Hall",
    "eventDate": "2026-05-15T18:00:00",
    "ownerName": "John Doe",
    "ownerContact": "9876543210",
    "photographerId": 1,
    "publicToken": "550e8400-e29b-41d4-a716-446655440000",
    "isActive": true,
    "createdAt": "2026-04-26T18:00:00",
    "updatedAt": "2026-04-26T18:00:00"
}
```

#### 3.2 Get Events by Photographer
**Endpoint**: `GET /api/events`

**Description**: Get all events for the authenticated photographer

**Authorization**: Required (Photographer/Admin role)

**Response**:
```json
[
    {
        "id": 1,
        "name": "Birthday Party",
        "venue": "Community Hall",
        "eventDate": "2026-05-15T18:00:00",
        "ownerName": "John Doe",
        "ownerContact": "9876543210",
        "photographerId": 1,
        "publicToken": "550e8400-e29b-41d4-a716-446655440000",
        "isActive": true,
        "createdAt": "2026-04-26T18:00:00",
        "updatedAt": "2026-04-26T18:00:00"
    }
]
```

#### 3.3 Get Event by Public Token
**Endpoint**: `GET /api/events/public/{publicToken}`

**Description**: Get event details using public token (No authentication required)

**Response**:
```json
{
    "id": 1,
    "name": "Birthday Party",
    "venue": "Community Hall",
    "eventDate": "2026-05-15T18:00:00",
    "ownerName": "John Doe",
    "ownerContact": "9876543210",
    "photographerId": 1,
    "publicToken": "550e8400-e29b-41d4-a716-446655440000",
    "isActive": true,
    "createdAt": "2026-04-26T18:00:00",
    "updatedAt": "2026-04-26T18:00:00"
}
```

### 4. Photo Upload

#### 4.1 Upload Photo with Face Detection
**Endpoint**: `POST /api/upload/event/{eventId}`

**Description**: Upload photo to event with automatic face detection and embedding extraction

**Authorization**: Required (Photographer/Admin role)

**Request**: `multipart/form-data`
- `file`: Image file (JPEG, PNG)

**Response**:
```json
{
    "success": true,
    "message": "Photo uploaded and processed successfully",
    "data": {
        "assetId": 1,
        "assetUrl": "uploads/event_1/1714478400000_sample_photo.jpg",
        "thumbnailUrl": "uploads/event_1/1714478400000_sample_photo_thumb.jpg",
        "size": 2048576,
        "facesDetected": 1
    }
}
```

**Status Codes**:
- `200`: Photo uploaded successfully
- `400`: No file provided or invalid file format
- `404`: Event not found
- `413`: File too large
- `422`: No face detected in image

### 5. Face Search

#### 5.1 Search Photos by Face
**Endpoint**: `POST /api/public/search/face/{publicToken}`

**Description**: Search photos in an event using face matching (No authentication required)

**Request**: `multipart/form-data`
- `file`: Selfie image for face matching

**Response**:
```json
[
    {
        "assetId": 1,
        "assetUrl": "uploads/event_1/1714478400000_photo1.jpg",
        "thumbnailUrl": "uploads/event_1/1714478400000_photo1_thumb.jpg",
        "similarityScore": 0.95
    },
    {
        "assetId": 2,
        "assetUrl": "uploads/event_1/1714478400001_photo2.jpg",
        "thumbnailUrl": "uploads/event_1/1714478400001_photo2_thumb.jpg",
        "similarityScore": 0.87
    }
]
```

**Status Codes**:
- `200`: Search completed successfully
- `400`: No file provided or invalid file format
- `404`: Event not found
- `422`: No face detected in selfie

## Error Response Format

All error responses follow this format:
```json
{
    "timestamp": "2026-04-26T18:00:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Detailed error message",
    "path": "/api/auth/request-otp"
}
```

## Common Error Codes

| Status Code | Description |
|-------------|-------------|
| 400 | Bad Request - Invalid input data |
| 401 | Unauthorized - Invalid or missing JWT token |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource not found |
| 413 | Payload Too Large - File size exceeds limit |
| 422 | Unprocessable Entity - Validation failed |
| 429 | Too Many Requests - Rate limit exceeded |
| 500 | Internal Server Error - Server error |

## Rate Limiting

- OTP requests: Limited to 3 requests per 10 minutes per email
- File uploads: Maximum file size 10MB
- Face search: Limited to 20 results per search

## File Upload Guidelines

- **Supported Formats**: JPEG, PNG
- **Maximum Size**: 10MB
- **Face Detection**: Images must contain at least one detectable face
- **Storage**: Files are stored locally in `uploads/event_{eventId}/` directory

## Face Search Algorithm

- Uses pgvector with cosine similarity
- Returns top 20 matching photos
- Similarity threshold: 0.7 (70% match)
- Results are sorted by similarity score (highest first)

## Environment Variables

Configure these in `application.properties`:

```properties
# JWT Configuration
jwt.secret=Q4W56GPqSmtryytcNlx9yDZhS8FI4bRlJrAAkwEDWjT
jwt.expiration=86400000

# OTP Configuration
otp.expiry.minutes=10
otp.max.attempts=3

# Python Face Service
python.face.service.url=http://localhost:8001/embed

# File Upload
upload.dir=uploads

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/grabpic
spring.datasource.username=your_username
spring.datasource.password=your_password

# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
```

## Testing with Postman

1. Import the `GrabPic-API-Collection.postman_collection.json` file into Postman
2. Set the environment variables:
   - `baseUrl`: `http://localhost:8080`
   - `photographerEmail`: Your test photographer email
   - `adminEmail`: Your test admin email
3. Follow the authentication flow:
   - Request OTP → Verify OTP → Copy JWT token
   - Set `jwtToken` variable with the received token
4. Test other endpoints with the JWT token automatically included

## Frontend Integration Notes

### JWT Token Management
- Store JWT token securely (e.g., localStorage, sessionStorage)
- Include token in all protected API calls
- Handle token expiration (24 hours)
- Implement token refresh logic if needed

### File Upload
- Use `FormData` for multipart file uploads
- Show progress indicators for large files
- Handle face detection errors gracefully
- Display uploaded images with thumbnails

### Face Search
- Allow users to upload selfie images
- Display search results with similarity scores
- Implement image preview on click
- Handle "no face detected" errors

### Error Handling
- Implement proper error boundaries
- Show user-friendly error messages
- Handle network timeouts gracefully
- Implement retry logic for failed requests

### Performance Considerations
- Implement image compression before upload
- Cache event data locally
- Use lazy loading for image galleries
- Implement pagination for large result sets

## Support

For API support and issues:
- Check the application logs for detailed error messages
- Verify database connectivity and pgvector extension
- Ensure Python face service is running on configured port
- Test with the provided Postman collection first
