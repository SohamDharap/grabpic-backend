# GrabPic Postman Collection

This directory contains the Postman collection and documentation for testing the GrabPic backend API.

## Files

- `GrabPic-API-Collection.postman_collection.json` - Complete Postman collection with all API endpoints
- `API-Documentation.md` - Comprehensive API documentation for frontend team
- `README.md` - This file with setup instructions

## Quick Setup

### 1. Import Collection into Postman

1. Open Postman
2. Click on "Import" in the top left
3. Select "File" tab
4. Choose `GrabPic-API-Collection.postman_collection.json`
5. Click "Import"

### 2. Configure Environment Variables

The collection uses environment variables for easy configuration:

1. In Postman, click on "Environments" tab
2. Create a new environment (e.g., "GrabPic Dev")
3. Add the following variables:

| Variable | Value | Description |
|----------|-------|-------------|
| `baseUrl` | `http://localhost:8080` | Backend API base URL |
| `photographerEmail` | `photographer@grabpic.com` | Test photographer email |
| `adminEmail` | `admin@grabpic.com` | Test admin email |
| `jwtToken` | *(leave empty)* | Will be set after OTP verification |

### 3. Test the API Flow

#### Step 1: Authentication
1. **Request OTP**: Use the "Request OTP" endpoint
2. **Verify OTP**: Use the "Verify OTP & Get JWT" endpoint
3. **Copy JWT Token**: From the response, copy the `token` value
4. **Set JWT Variable**: Update the `jwtToken` environment variable

#### Step 2: Admin Operations (if using admin account)
1. **Create Photographer**: Use the "Create Photographer" endpoint
2. **Get All Photographers**: Use the "Get All Photographers" endpoint

#### Step 3: Event Management
1. **Create Event**: Use the "Create Event" endpoint
2. **Get Events**: Use the "Get Events by Photographer" endpoint
3. **Get Public Event**: Use the "Get Event by Public Token" endpoint

#### Step 4: Photo Upload
1. **Upload Photo**: Use the "Upload Photo with Face Detection" endpoint
2. **Note**: You'll need a sample image file for testing

#### Step 5: Face Search
1. **Search Photos**: Use the "Search Photos by Face" endpoint
2. **Note**: You'll need a selfie image for testing

## Prerequisites

### Backend Setup
- Spring Boot application running on `localhost:8080`
- PostgreSQL database with pgvector extension
- Python face service running on `localhost:8001`

### Sample Data
- Test user accounts in database:
  - Admin: `admin@grabpic.com`
  - Photographer: `photographer@grabpic.com`

### Test Images
- Sample photos with faces for upload testing
- Selfie images for face search testing

## Collection Structure

The collection is organized into folders:

1. **Authentication**
   - Request OTP
   - Verify OTP & Get JWT

2. **Admin Management**
   - Create Photographer
   - Get All Photographers

3. **Event Management**
   - Create Event
   - Get Events by Photographer
   - Get Event by Public Token

4. **Photo Upload**
   - Upload Photo with Face Detection

5. **Face Search**
   - Search Photos by Face

## Common Issues & Solutions

### JWT Token Issues
- **Problem**: "No qualifying bean of type 'ObjectMapper'"
- **Solution**: Ensure the JacksonConfig is properly configured

### File Upload Issues
- **Problem**: "No face detected in the image"
- **Solution**: Use clear photos with visible faces
- **Problem**: "File too large"
- **Solution**: Ensure images are under 10MB

### Face Search Issues
- **Problem**: "Event not found"
- **Solution**: Verify the publicToken is correct and event exists
- **Problem**: "No matching photos found"
- **Solution**: Ensure photos with faces have been uploaded to the event

### Database Issues
- **Problem**: "Schema validation errors"
- **Solution**: Ensure pgvector extension is installed and tables match schema

## Testing Tips

1. **Use Collection Runner**: Run multiple requests in sequence
2. **Test Variables**: Use environment variables for dynamic values
3. **Response Validation**: Check response codes and body formats
4. **Error Testing**: Test with invalid data to verify error handling

## Frontend Integration

Share the `API-Documentation.md` file with the frontend team. It includes:
- Complete API endpoint documentation
- Request/response examples
- Error handling guidelines
- Integration best practices

## Support

For any issues:
1. Check the backend application logs
2. Verify all prerequisites are met
3. Test with the provided examples first
4. Refer to the API documentation

## Version Control

This Postman collection is tracked in Git. When updating:
1. Test all endpoints after changes
2. Update sample responses if needed
3. Update documentation accordingly
4. Commit changes with descriptive messages
