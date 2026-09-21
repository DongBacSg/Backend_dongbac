# Dong Bac Sai Gon Backend

Backend Phase 8 for Dong Bac Sai Gon.

This repository is a Spring Boot modular monolith for the public website and internal management system. Phase 2 added authentication and security. Phase 3 added a generic approval workflow and persistent audit trail. Phase 4 added Cloudinary direct-upload media management and site content management. Phase 5 added Category management and a revisioned Product catalog. Phase 6 added a revisioned Article CMS. Phase 7 added internal Customer Lead management and a role-aware operational Dashboard. Phase 8 hardened security, testing, and production operations. A later V8 compatibility patch adds public contact intake, two Article types, and manufacturing service details.

## Stack

- Java 17
- Spring Boot 3.5.16
- Maven Wrapper
- Spring Web
- Spring Data JPA
- Spring Security
- Spring OAuth2 Resource Server / JOSE JWT support
- PostgreSQL JDBC
- Flyway
- Bean Validation
- Spring Boot Actuator
- Springdoc OpenAPI / Swagger UI
- Cloudinary Java SDK
- jsoup HTML sanitizer
- Docker / Render

No Keycloak, Auth0, Firebase Auth, Supabase Auth, Redis, Kafka, Spring Session, H2, frontend integration, public Lead intake, Recruitment CMS, Customer accounts, Orders, or Payments are included.

## Architecture

Main package:

```text
com.dongbacsaigon.backend
├── common
│   ├── config
│   ├── exception
│   ├── response
│   ├── util
│   └── web
├── auth
│   ├── config
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   ├── security
│   └── service
├── user
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
├── approval
├── media
├── site
├── catalog
├── article
├── lead
├── dashboard
└── audit
```

Route conventions:

- Public APIs: `/api/public/**`
- Internal management APIs: `/api/admin/**`

Phase 8 includes auth, staff-account management, Approval Center APIs, persistent audit logs, Cloudinary-backed media, site management, revisioned Products and Articles, internal Customer Leads, database-backed Dashboard aggregates, and final production hardening.

## Roles

There are exactly two roles:

- `ADMIN`: all STAFF capabilities plus Category management, Approval Center decisions, Product and Article lifecycle commands, Lead assignment, STAFF-account management, and site management.
- `STAFF`: Category read access, Product and Article draft/revision authoring, internal Lead management/status updates, Dashboard access, and media library access for owned media.

There is no public registration, public login, customer account, social login, or forgot-password flow.

## Database

The application connects directly to Supabase PostgreSQL over JDBC through the Supabase Session Pooler:

```text
jdbc:postgresql://<SESSION_POOLER_HOST>:5432/postgres?sslmode=require
```

Application schema:

```text
dongbac
```

Flyway owns schema changes. Hibernate remains validation-only:

```text
spring.jpa.hibernate.ddl-auto=validate
```

Migrations:

- `V1__initialize_dongbac_schema.sql`: creates the `dongbac` schema only.
- `V2__create_users_and_auth.sql`: creates Phase 2 `users` and `refresh_tokens`.
- `V3__create_approval_and_audit.sql`: creates Phase 3 `approval_requests` and `audit_logs`.
- `V4__create_media_and_site_management.sql`: creates Phase 4 media, upload-intent, and site-management tables.
- `V5__create_catalog.sql`: creates Phase 5 Category, Product identity, Product revision, image relationship, and related-Product tables.
- `V6__create_article_cms.sql`: creates Phase 6 Article identity, Article revision, and Article media relationship tables.
- `V7__create_customer_leads.sql`: creates the Phase 7 internal `customer_leads` table. Dashboard data is computed and has no persistence table.
- `V8__frontend_compatibility_features.sql`: permits anonymous-origin Leads, adds Lead subject and the two Article types, and creates manufacturing service detail content.

## Phase 2 Schema

`dongbac.users` stores internal accounts:

- `id UUID primary key`
- `email varchar(320) unique not null`
- `password_hash varchar(255) not null`
- `full_name varchar(160) not null`
- `role varchar(16) not null` constrained to `ADMIN`, `STAFF`
- `status varchar(16) not null` constrained to `ACTIVE`, `LOCKED`
- `must_change_password boolean not null`
- `failed_login_attempts integer not null`
- `locked_until timestamptz`
- `last_login_at timestamptz`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`
- `created_by UUID`

`dongbac.refresh_tokens` stores refresh-token metadata:

- `id UUID primary key`
- `user_id UUID not null`
- `token_hash varchar(64) unique not null`
- `expires_at timestamptz not null`
- `revoked_at timestamptz`
- `created_at timestamptz not null`
- `last_used_at timestamptz`
- `replacement_token_id UUID`

The raw refresh token is never stored. Only SHA-256 hex hashes are persisted.

## Phase 3 Schema

`dongbac.approval_requests` stores generic approval records. Phase 5 uses `PRODUCT` and Phase 6 uses `ARTICLE`:

- `id UUID primary key`
- `resource_type varchar(32) not null` constrained to `PRODUCT`, `ARTICLE`
- `resource_id UUID not null`
- `resource_version bigint not null`
- `status varchar(32) not null` constrained to `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`
- `submitted_by UUID not null`
- `submitted_at timestamptz not null`
- `reviewed_by UUID`
- `reviewed_at timestamptz`
- `review_note text`
- `created_at timestamptz not null`
- `updated_at timestamptz not null`
- `version bigint not null`

`resource_id` intentionally has no database foreign key because approvals support multiple resource types. For Products and Articles it stores the stable aggregate ID, while `resource_version` stores the revision number. A partial unique index prevents duplicate `PENDING` requests for the same resource version.

`dongbac.audit_logs` stores immutable audit records:

- `id UUID primary key`
- `occurred_at timestamptz not null`
- `actor_user_id UUID`
- `actor_email_snapshot varchar(320)`
- `actor_role_snapshot varchar(16)`
- `action varchar(64) not null`
- `target_type varchar(64) not null`
- `target_id UUID`
- `outcome varchar(16) not null`
- `reason text`
- `correlation_id varchar(100)`
- `ip_address varchar(128)`
- `user_agent text`
- `created_at timestamptz not null`

Audit records are append-only. There are no update or delete APIs for audit logs.

## Phase 4 Schema

`dongbac.media_upload_intents` stores short-lived direct-upload registration intents:

- `id UUID primary key`
- `public_id varchar(160) not null`
- `folder varchar(255) not null`
- `media_type varchar(16) not null` constrained to `IMAGE`, `VIDEO`
- `original_filename varchar(255)`
- `requested_by UUID not null`
- `status varchar(16) not null` constrained to `PENDING`, `COMPLETED`, `FAILED`, `EXPIRED`
- `expires_at timestamptz not null`
- `completed_at timestamptz`
- `created_at timestamptz not null`

`dongbac.media` stores verified Cloudinary metadata only. Binary files are uploaded from the browser directly to Cloudinary, not proxied through this backend:

- `id UUID primary key`
- `asset_id varchar(255) unique not null`
- `public_id varchar(255) not null`
- `resource_type varchar(16) not null` constrained to `IMAGE`, `VIDEO`
- `format varchar(32) not null`
- `secure_url text not null`
- `width integer`
- `height integer`
- `bytes bigint not null`
- `duration_seconds numeric(12, 3)`
- `folder varchar(255)`
- `original_filename varchar(255)`
- `alt_text varchar(255)`
- `status varchar(16) not null` constrained to `ACTIVE`, `DELETED`
- `uploaded_by UUID not null`
- `deleted_at timestamptz`
- `deleted_by UUID`
- `version bigint not null`

`asset_id` is unique, and `(resource_type, public_id)` is unique. The backend generates Cloudinary `public_id` values and verifies the uploaded Cloudinary resource before inserting a media row.

Site-management tables:

- `dongbac.site_banners`: homepage/banner media, alt text, sort order, active flag.
- `dongbac.site_partners`: partner name, logo media, website URL, sort order, active flag.
- `dongbac.site_settings`: singleton company settings, contact details, social links, logo, TVC URL.
- `dongbac.manufacturing_page`: singleton manufacturing page title, introduction, hero image.
- `dongbac.manufacturing_sections`: manufacturing content sections with optional image, sort order, active flag.

Phase 4 itself does not create catalog, Article, or Lead tables. Those are introduced by V5, V6, and V7 respectively. V8 extends those existing tables; it does not create separate Recruitment or public Contact tables.

## Phase 5 Catalog Schema

V5 creates exactly these catalog tables:

- `dongbac.categories`: normalized unique slug, ordering, active flag, audit ownership, and optimistic version.
- `dongbac.products`: stable Product identity, Product-level publication status, monotonically increasing latest revision number, and current published revision pointer.
- `dongbac.product_revisions`: immutable submitted snapshots containing Category, name, revision-specific slug, descriptions/content, SEO fields, workflow metadata, and publishing metadata.
- `dongbac.product_revision_images`: ordered references to existing Phase 4 Media rows. Cloudinary metadata is not duplicated.
- `dongbac.product_revision_related_products`: ordered references from one revision to stable related Product identities.

Product publication status is one of:

```text
DRAFT
PUBLISHED
UNPUBLISHED
ARCHIVED
```

Product revision status is one of:

```text
DRAFT
PENDING_REVIEW
REJECTED
PUBLISHED
ARCHIVED
```

There is no Product or Product revision status named `APPROVED`. `APPROVED` belongs only to the Approval Request. An approved Product revision transitions to `PUBLISHED`.

The Product identity is stable while content is versioned. STAFF never edits the current public revision. A new edit creates a new `DRAFT`; after submission that exact snapshot is immutable. Rejected revisions remain historical, and revising rejected content creates a new numbered draft. When a newer revision is approved, the previous public revision becomes `ARCHIVED` and the Product pointer changes atomically.

Revision numbers are generated server-side while the Product row is locked. Partial unique indexes enforce at most one DRAFT, one PENDING revision, one primary image per revision, and globally unique currently published slugs. Application validation runs again during approval for active Category, active IMAGE Media, primary-image integrity, and slug conflicts.

Category permissions:

- `ADMIN`: create, update, safely activate/deactivate, and delete only unused Categories.
- `STAFF`: read/select existing Categories only.
- Public: read active Categories only.

Product authoring permissions:

- `ADMIN` and `STAFF`: create Product draft, update DRAFT only, create the next revision, manage draft Media relationships and related Products, submit to review, and read revision history.
- `ADMIN` only: approve/reject through the existing Approval Center, unpublish, republish the unchanged approved revision, and archive.
- `STAFF` cannot approve, publish, unpublish, republish, archive, or mutate submitted/published content.

Product images accept only existing active Phase 4 Media with `resourceType=IMAGE`. Product APIs accept `mediaId`; they never accept multipart files, base64, Cloudinary URLs, or Cloudinary public IDs. Any Media referenced by any Product revision, including historical revisions, is protected from deletion.

Related Products may be stored while unpublished, but public responses include only related Products that are currently `PUBLISHED` with an active Category and valid current published revision.

## Phase 6 Article CMS

V6 creates exactly these Article tables:

- `dongbac.articles`: stable Article identity, publication status, latest revision number, and current published revision pointer.
- `dongbac.article_revisions`: immutable submitted content snapshots with type, title, slug, summary, sanitized content, SEO fields, workflow metadata, and publishing metadata.
- `dongbac.article_revision_media`: ordered `FEATURED` or `CONTENT` references to existing Phase 4 Media rows. Cloudinary metadata is not duplicated.

Supported Article types are exactly:

```text
INTERNAL_ACTIVITY
NEWS
KNOWLEDGE
RECRUITMENT
ANNOUNCEMENT
```

Article publication status is one of:

```text
DRAFT
PUBLISHED
UNPUBLISHED
ARCHIVED
```

Article revision status is one of:

```text
DRAFT
PENDING_REVIEW
REJECTED
PUBLISHED
ARCHIVED
```

There is no Article or Article revision status named `APPROVED`. `APPROVED` belongs only to the Approval Request. An approved Article revision transitions to `PUBLISHED`.

The Article identity is stable while its content is versioned. A submitted revision is immutable. STAFF edits a new DRAFT while the public site continues reading only `Article.currentPublishedRevision`. Rejection preserves the current public revision; approving a newer revision archives the previous public snapshot and switches the pointer atomically.

Revision numbers are generated server-side while the Article row is locked. Application checks and partial unique indexes protect active working revisions, the featured-image relationship, and public slug uniqueness. Slugs use the Phase 5 Vietnamese normalization utility.

Article content accepts a small HTML subset and is sanitized server-side with jsoup. Allowed elements are `p`, `br`, `strong`, `b`, `em`, `i`, `u`, `s`, `ul`, `ol`, `li`, `blockquote`, `h2`, `h3`, `h4`, and `a`. Anchors accept only `href` and `title`, with `http`, `https`, or `mailto` protocols. Scripts, event handlers, and unsafe URL protocols are removed before persistence.

Article media uses existing active Media rows with `resourceType=IMAGE`. Article APIs accept only `mediaId`, `usageType`, `sortOrder`, and optional caption; they do not accept file bytes or Cloudinary credentials. Each revision can have at most one `FEATURED` image. `CONTENT` images are returned by ascending `sortOrder`. Media referenced by any Article revision, including rejected and archived history, is protected from deletion.

Permissions:

- `ADMIN` and `STAFF`: create Article drafts, update DRAFT revisions, create a next revision, submit to review, and read Article/revision history.
- `ADMIN` only: hard-delete a safe never-submitted draft, approve/reject through Approval Center, unpublish, republish the unchanged approved revision, and archive.
- Public: list or resolve only the current `PUBLISHED` revision of an Article whose publication status is also `PUBLISHED`.

## Phase 7 Customer Leads

V7 creates `dongbac.customer_leads`; V8 adds optional `subject` and makes `created_by` nullable for public contact. A Lead stores contact/business text, current status, optional assignment, timestamps, and an optimistic `version`. There is no hard-delete endpoint, public Lead list/detail endpoint, Lead event table, Customer account, sales pipeline, or financial field.

Lead statuses are exactly:

```text
NEW
CONTACTED
IN_PROGRESS
COMPLETED
CANCELLED
SPAM
```

Creation always starts at `NEW`. Internal creation retains the authenticated `createdBy`; public contact has `createdBy=null`. At least one trimmed phone number or valid email address is required. Email is lowercased after trimming; phone accepts reasonable local/international digits, `+`, spaces, hyphens, and parentheses. The business has no strict transition matrix, so authorized users may move between any valid statuses. A status no-op returns cleanly without a duplicate audit event.

Lead permissions:

- `ADMIN` and `STAFF`: create, list, read, replace editable contact/business text, edit internal notes, and change status.
- `ADMIN` only: assign, reassign, or unassign an active `ADMIN` or `STAFF` account.
- Public and anonymous users: only `POST /api/public/contact` for anonymous intake; no Lead data read access.

Lead audit actions include `PUBLIC_CONTACT_CREATED`, `LEAD_CREATED`, `LEAD_UPDATED`, `LEAD_STATUS_CHANGED`, `LEAD_ASSIGNED`, and `LEAD_UNASSIGNED`. Public contact audit stores the Lead ID with an anonymous actor and no PII. Audit reasons contain only concise status or assignment identifiers; customer phone, email, message, and internal note are not duplicated into audit metadata.

## Phase 7 Dashboard

`GET /api/admin/dashboard/summary` computes operational counts directly from repository `COUNT`/`GROUP BY` queries. No Dashboard table or fake statistics are used. It returns Lead totals by `LeadStatus`, pending approvals, Product identities by publication status, Article identities by publication status, and active Media count.

Product and Article counts query their stable identity tables, not revision tables. `ADMIN` receives the global pending Approval count. `STAFF` receives only pending requests submitted by that authenticated user, preserving the Phase 3 ownership boundary. Revenue, orders, payments, traffic, conversion rates, and time-series analytics are intentionally absent.

## Approval Workflow

Approval request statuses are separate from Product and Article revision publishing states.

Approval request statuses:

```text
PENDING
APPROVED
REJECTED
CANCELLED
```

Product and Article revisions use their own lifecycle:

```text
DRAFT -> PENDING_REVIEW -> PUBLISHED
                       \-> REJECTED
```

When an Admin approves a request, the matching production handler transitions the exact `PENDING_REVIEW` revision to `PUBLISHED` and updates the aggregate's current published pointer in the same transaction. `ProductApprovalHandler` handles `PRODUCT`; `ArticleApprovalHandler` handles `ARTICLE`.

Internal integration contract:

```java
submitForReview(resourceType, resourceId, resourceVersion, submittedBy)
```

Modules register an `ApprovalResourceHandler` that supports a resource type and implements:

- `validateCanSubmit(...)`
- `onApproved(...)`
- `onRejected(...)`

Phase 6 registers both production handlers. Article submission uses the stable Article ID as `resourceId` and the server-generated revision number as `resourceVersion`; it never uses the ArticleRevision UUID as the approval resource ID.

Approval Center APIs:

```text
GET  /api/admin/approvals
GET  /api/admin/approvals/{id}
POST /api/admin/approvals/{id}/approve
POST /api/admin/approvals/{id}/reject
```

These routes are `ADMIN` only. Approve accepts an optional note. Reject requires a non-blank reason between 3 and 1000 characters.

Own approval history APIs:

```text
GET /api/admin/approvals/mine
GET /api/admin/approvals/mine/{id}
```

`ADMIN` and `STAFF` can use these routes. A STAFF user can see only requests submitted by that same user.

Approve/reject operations load the request with a pessimistic write lock and also use JPA optimistic `@Version`. A request already reviewed returns `409 Conflict`.

## Audit Trail

Audit actions are standardized backend enum values, not client-supplied strings:

```text
AUTH_LOGIN_SUCCESS
AUTH_LOGIN_FAILURE
AUTH_LOGOUT
AUTH_PASSWORD_CHANGED
STAFF_CREATED
STAFF_UPDATED
STAFF_LOCKED
STAFF_UNLOCKED
STAFF_PASSWORD_RESET
APPROVAL_SUBMITTED
APPROVAL_APPROVED
APPROVAL_REJECTED
APPROVAL_CANCELLED
ADMIN_BOOTSTRAPPED
MEDIA_UPLOAD_COMPLETED
MEDIA_METADATA_UPDATED
MEDIA_DELETED
SITE_BANNER_CREATED
SITE_BANNER_UPDATED
SITE_BANNER_DELETED
SITE_PARTNER_CREATED
SITE_PARTNER_UPDATED
SITE_PARTNER_DELETED
SITE_SETTINGS_UPDATED
MANUFACTURING_PAGE_UPDATED
MANUFACTURING_SECTION_CREATED
MANUFACTURING_SECTION_UPDATED
MANUFACTURING_SECTION_DELETED
CATEGORY_CREATED
CATEGORY_UPDATED
CATEGORY_DEACTIVATED
CATEGORY_ACTIVATED
CATEGORY_DELETED
PRODUCT_CREATED
PRODUCT_REVISION_CREATED
PRODUCT_REVISION_UPDATED
PRODUCT_REVISION_SUBMITTED
PRODUCT_REVISION_PUBLISHED
PRODUCT_REVISION_REJECTED
PRODUCT_UNPUBLISHED
PRODUCT_REPUBLISHED
PRODUCT_ARCHIVED
PRODUCT_DRAFT_DELETED
ARTICLE_CREATED
ARTICLE_REVISION_CREATED
ARTICLE_REVISION_UPDATED
ARTICLE_REVISION_SUBMITTED
ARTICLE_REVISION_PUBLISHED
ARTICLE_REVISION_REJECTED
ARTICLE_UNPUBLISHED
ARTICLE_REPUBLISHED
ARTICLE_ARCHIVED
ARTICLE_DRAFT_DELETED
LEAD_CREATED
LEAD_UPDATED
LEAD_STATUS_CHANGED
LEAD_ASSIGNED
LEAD_UNASSIGNED
```

Audit target types:

```text
AUTH
USER
STAFF
PRODUCT
ARTICLE
APPROVAL_REQUEST
MEDIA
BANNER
PARTNER
SITE_SETTINGS
MANUFACTURING
CATEGORY
PRODUCT_REVISION
ARTICLE_REVISION
LEAD
SYSTEM
```

Audit outcomes:

```text
SUCCESS
FAILURE
```

Successful business/security audit events are persisted after transaction commit. Failure events such as failed login are written independently so they are not lost with auth rollback. Audit persistence failures are logged safely and do not expose passwords, tokens, cookies, Authorization headers, or secrets.

Admin audit APIs:

```text
GET /api/admin/audit
GET /api/admin/audit/{id}
```

These routes are `ADMIN` only. They support pagination and filters for action, actor user, target type, target id, outcome, and date range. Results sort by `occurredAt DESC`.

## Correlation ID

Every request receives an `X-Correlation-ID` response header. If a safe incoming `X-Correlation-ID` exists, it is reused; otherwise a UUID is generated. The same correlation ID is available to audit records.

## Media Management

Phase 4 uses direct signed uploads to Cloudinary:

1. An authenticated `ADMIN` or `STAFF` asks the backend for signed upload parameters.
2. The browser uploads the binary file directly to Cloudinary using those parameters.
3. The browser tells the backend the upload intent is complete.
4. The backend fetches the Cloudinary resource by server-generated `public_id`, validates type, folder, format, and size, then stores metadata in `dongbac.media`.

The frontend never receives `CLOUDINARY_API_SECRET`. Render never receives normal media file streams.

Allowed formats:

```text
IMAGE: jpg, jpeg, png, webp, avif
VIDEO: mp4, webm, mov
```

Default size limits:

```text
MEDIA_IMAGE_MAX_BYTES=10485760
MEDIA_VIDEO_MAX_BYTES=209715200
MEDIA_UPLOAD_INTENT_MINUTES=15
```

Media library APIs:

```text
POST   /api/admin/media/uploads/signature
POST   /api/admin/media/uploads/{intentId}/complete
GET    /api/admin/media
GET    /api/admin/media/{id}
PATCH  /api/admin/media/{id}
DELETE /api/admin/media/{id}
```

`ADMIN` and `STAFF` can create upload intents, complete uploads, list media, and read media metadata. `STAFF` can update or delete only media they uploaded. `ADMIN` can update or delete any unused media. A referenced media item cannot be deleted.

Example signed-upload request:

```json
{
  "mediaType": "IMAGE",
  "originalFilename": "factory-line.webp"
}
```

Example signed-upload response:

```json
{
  "intentId": "...",
  "cloudName": "cloud-name",
  "apiKey": "cloudinary-api-key",
  "timestamp": 1790000000,
  "signature": "...",
  "folder": "dongbac/media/2026/09",
  "publicId": "...",
  "resourceType": "image",
  "uploadUrl": "https://api.cloudinary.com/v1_1/cloud-name/image/upload",
  "expiresAt": "2026-09-17T00:15:00Z"
}
```

The browser should post `timestamp`, `api_key`, `signature`, `folder`, `public_id`, and `file` to `uploadUrl`. After Cloudinary upload succeeds, call:

```text
POST /api/admin/media/uploads/{intentId}/complete
```

No client-supplied Cloudinary metadata is trusted for database registration.

## Site Management

Admin site-management APIs:

```text
GET    /api/admin/site/banners
POST   /api/admin/site/banners
PATCH  /api/admin/site/banners/{id}
DELETE /api/admin/site/banners/{id}

GET    /api/admin/site/partners
POST   /api/admin/site/partners
PATCH  /api/admin/site/partners/{id}
DELETE /api/admin/site/partners/{id}

GET    /api/admin/site/settings
PUT    /api/admin/site/settings

GET    /api/admin/site/manufacturing
PUT    /api/admin/site/manufacturing
GET    /api/admin/site/manufacturing/sections
POST   /api/admin/site/manufacturing/sections
PATCH  /api/admin/site/manufacturing/sections/{id}
DELETE /api/admin/site/manufacturing/sections/{id}
GET    /api/admin/site/manufacturing/services?page=0&size=20&search=&active=
GET    /api/admin/site/manufacturing/services/{id}
POST   /api/admin/site/manufacturing/services
PATCH  /api/admin/site/manufacturing/services/{id}
DELETE /api/admin/site/manufacturing/services/{id}
```

These routes are `ADMIN` only. Site content can reference only active image media. External URLs are accepted only when they use `http` or `https`.

Public site read APIs:

```text
GET /api/public/site/banners
GET /api/public/site/partners
GET /api/public/site/settings
GET /api/public/site/manufacturing
GET /api/public/site/manufacturing/services
GET /api/public/site/manufacturing/services/{slug}
```

Public endpoints require no authentication, return only public-safe media fields, and expose only active banners, partners, manufacturing sections, and services. Service list omits full content; detail by slug returns active content or 404. Admin service management is ADMIN-only and uses existing active image Media; referenced Media cannot be deleted.

## Catalog APIs

Category management:

```text
GET    /api/admin/catalog/categories
GET    /api/admin/catalog/categories/{id}
POST   /api/admin/catalog/categories
PATCH  /api/admin/catalog/categories/{id}
DELETE /api/admin/catalog/categories/{id}
```

The two GET routes allow `ADMIN` and `STAFF`. Mutations are `ADMIN` only. Category search covers name and slug, page size is bounded to 100, deletion is blocked by any Product revision reference, and deactivation is blocked while currently public Products use the Category.

Product and revision management:

```text
GET    /api/admin/catalog/products
GET    /api/admin/catalog/products/{productId}
POST   /api/admin/catalog/products
DELETE /api/admin/catalog/products/{productId}

GET    /api/admin/catalog/products/{productId}/revisions
POST   /api/admin/catalog/products/{productId}/revisions
GET    /api/admin/catalog/products/{productId}/revisions/{revisionId}
PATCH  /api/admin/catalog/products/{productId}/revisions/{revisionId}
POST   /api/admin/catalog/products/{productId}/revisions/{revisionId}/submit

POST   /api/admin/catalog/products/{productId}/unpublish
POST   /api/admin/catalog/products/{productId}/publish
POST   /api/admin/catalog/products/{productId}/archive
```

Product list supports `search`, `categoryId`, `publicationStatus`, `revisionStatus`, and bounded pagination. Hard delete is allowed only for a never-submitted, never-published, unreferenced draft with no approval history. Lifecycle routes are `ADMIN` only. `/publish` only restores the already approved current revision from `UNPUBLISHED`; it never publishes a DRAFT.

Submitting a revision calls the existing Approval Service with:

```text
resourceType = PRODUCT
resourceId = Product ID
resourceVersion = revision number
```

Approval and rejection continue through the centralized endpoints:

```text
POST /api/admin/approvals/{approvalId}/approve
POST /api/admin/approvals/{approvalId}/reject
```

Public catalog:

```text
GET /api/public/catalog/categories
GET /api/public/catalog/categories/{categorySlug}
GET /api/public/catalog/products
GET /api/public/catalog/products/{productSlug}
GET /api/public/catalog/categories/{categorySlug}/products
GET /api/public/catalog/categories/{categorySlug}/products/{productSlug}
```

Public Product endpoints return only `products.publication_status=PUBLISHED` plus the current `PUBLISHED` revision under an active Category. DRAFT, PENDING_REVIEW, REJECTED, UNPUBLISHED, and ARCHIVED content is never resolved by public slug or included in public lists. Product content is persisted and returned as text; Phase 5 does not implement a generic HTML page builder or script execution.

## Article APIs

Article and revision management:

```text
GET    /api/admin/articles
GET    /api/admin/articles/{articleId}
POST   /api/admin/articles
DELETE /api/admin/articles/{articleId}

GET    /api/admin/articles/{articleId}/revisions
POST   /api/admin/articles/{articleId}/revisions
GET    /api/admin/articles/{articleId}/revisions/{revisionId}
PATCH  /api/admin/articles/{articleId}/revisions/{revisionId}
POST   /api/admin/articles/{articleId}/revisions/{revisionId}/submit

POST   /api/admin/articles/{articleId}/unpublish
POST   /api/admin/articles/{articleId}/publish
POST   /api/admin/articles/{articleId}/archive
```

The admin list supports `search`, `articleType`, `publicationStatus`, `revisionStatus`, and pagination bounded to 100 rows. Hard delete is `ADMIN` only and succeeds only for a never-submitted, never-published draft with no approval history. Lifecycle routes are `ADMIN` only. `/publish` only restores the unchanged approved current revision from `UNPUBLISHED`; it cannot publish a DRAFT.

Submitting an Article revision calls the existing Approval Service with:

```text
resourceType = ARTICLE
resourceId = Article ID
resourceVersion = revision number
```

Approval and rejection use the centralized Approval Center endpoints. There are no Article-specific approve/reject routes.

Public Articles:

```text
GET /api/public/articles
GET /api/public/articles/internal-activities
GET /api/public/articles/{slug}
```

The generic list accepts `type` (including `RECRUITMENT` and `ANNOUNCEMENT`), `search`, `page`, and `size`. Both new types use the same revision and Approval Center workflow. List responses are lightweight and omit the content body. Detail responses include sanitized content, one optional featured image, and ordered content images. DRAFT, PENDING_REVIEW, REJECTED, UNPUBLISHED, and ARCHIVED content is never included or resolved by public slug.

## Lead And Dashboard APIs

Internal Lead management:

```text
POST  /api/admin/leads
GET   /api/admin/leads
GET   /api/admin/leads/{leadId}
PATCH /api/admin/leads/{leadId}
PATCH /api/admin/leads/{leadId}/status
PATCH /api/admin/leads/{leadId}/assignment
```

The first five operations allow `ADMIN` and `STAFF`; assignment is `ADMIN` only. General update replaces only `fullName`, `phone`, `email`, `companyName`, `message`, and `internalNote`. Status and assignment have dedicated endpoints. There is deliberately no DELETE route or public Lead read endpoint.

Public contact intake:

```text
POST /api/public/contact
```

The anonymous request contains `name`, optional `phone` and `email` (at least one required), optional `subject`, and required `message`. It returns `201` with only an acknowledgement message. Client-supplied Lead status, assignment, internal note, creator, and timestamps are not accepted as DTO fields. Existing JSON request-size, validation, correlation-ID, and error handling apply. Edge rate limiting and/or CAPTCHA remain a future deployment-security TODO; this patch adds no new provider or distributed infrastructure.

Lead list accepts `page`, `size`, `search`, `status`, `assignedTo`, and `unassigned`. Search covers name, phone, email, and company but not internal notes. `assignedTo` together with `unassigned=true` returns `400`. Page size is bounded to 100 and ordering is `createdAt DESC, id DESC`.

Dashboard summary:

```text
GET /api/admin/dashboard/summary
```

Both roles can read the summary. Lead/Product/Article/Media values come from real database counts. The Approval pending count is global for `ADMIN` and ownership-scoped for `STAFF`.

## Authentication Flow

Access tokens are short-lived JWTs signed with HMAC SHA-256 using `JWT_SECRET_BASE64`. The secret must decode to at least 32 random bytes.

JWT claims include:

- `sub`: user UUID
- `role`: `ADMIN` or `STAFF`
- `email`: normalized email
- `must_change_password`
- `iss`
- `iat`
- `exp`
- `jti`

Default access-token lifetime:

```text
JWT_ACCESS_TOKEN_MINUTES=15
```

Refresh tokens are long-lived opaque random tokens. The raw token is returned only as an HttpOnly cookie named by `AUTH_REFRESH_COOKIE_NAME`; the API JSON response never includes it.

Default refresh lifetime:

```text
REFRESH_TOKEN_DAYS=7
```

Refresh rotates on every successful `/api/admin/auth/refresh` call. The old token is revoked and linked to the replacement token. Reusing the old token fails.

Because access JWTs are stateless, manual account lock and password reset revoke refresh tokens immediately, while an existing access token may remain usable until its short expiry. No Redis JWT blacklist is implemented in Phase 2.

## Cookies And CSRF Guard

Refresh cookie path:

```text
/api/admin/auth
```

Development defaults:

```text
AUTH_REFRESH_COOKIE_SECURE=false
AUTH_REFRESH_COOKIE_SAME_SITE=Lax
```

Production defaults:

```text
AUTH_REFRESH_COOKIE_SECURE=true
AUTH_REFRESH_COOKIE_SAME_SITE=None
```

Cookie-authenticated refresh/logout endpoints require:

```text
X-CSRF-Guard: 1
```

This lightweight guard prevents simple cross-site form abuse while preserving a stateless Bearer-token API model. Login does not require this header.

## Auth Endpoints

```text
POST /api/admin/auth/login
POST /api/admin/auth/refresh
POST /api/admin/auth/logout
GET  /api/admin/auth/me
POST /api/admin/auth/change-password
```

Login response includes the access token and safe user fields only:

```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "...",
    "email": "admin@example.com",
    "fullName": "Admin",
    "role": "ADMIN",
    "mustChangePassword": false
  }
}
```

No password hash, plaintext password, raw refresh token, database secret, or JWT secret is returned.

## Staff Management Endpoints

All staff-management routes require `ROLE_ADMIN`:

```text
GET    /api/admin/staff
GET    /api/admin/staff/{id}
POST   /api/admin/staff
PATCH  /api/admin/staff/{id}
PATCH  /api/admin/staff/{id}/lock
PATCH  /api/admin/staff/{id}/unlock
POST   /api/admin/staff/{id}/reset-password
```

`/api/admin/staff/**` manages only `STAFF` accounts. It cannot create, lock, unlock, or reset an `ADMIN`.

List supports bounded pagination:

```text
page=0
size=20
search=<email-or-name>
status=ACTIVE|LOCKED
```

Maximum page size is 100.

## Passwords

Passwords use Spring Security BCrypt with strength 12.

Policy:

- 12 to 128 characters
- at least one letter
- at least one digit

Temporary STAFF passwords set `must_change_password=true`. While that JWT claim is true, authenticated admin APIs are blocked except:

- `GET /api/admin/auth/me`
- `POST /api/admin/auth/change-password`
- `POST /api/admin/auth/logout`
- `POST /api/admin/auth/refresh`

## Login Protection

Login failures never reveal whether an email exists. Unknown email and wrong password both return:

```text
Invalid email or password.
```

Default temporary lockout:

```text
AUTH_MAX_FAILED_ATTEMPTS=5
AUTH_LOCK_MINUTES=15
```

Temporary lockout is separate from administrator account status `LOCKED`.

## First ADMIN Bootstrap

Optional startup bootstrap variables:

```text
BOOTSTRAP_ADMIN_EMAIL
BOOTSTRAP_ADMIN_PASSWORD
BOOTSTRAP_ADMIN_NAME
```

Startup behavior:

1. If any `ADMIN` exists, do nothing.
2. If no `ADMIN` exists and all bootstrap values are valid, create one active `ADMIN`.
3. If no `ADMIN` exists and bootstrap values are missing or incomplete, start normally and log a safe warning.

Bootstrap never logs the password, never resets an existing ADMIN password, and never creates duplicate ADMIN accounts.

Recommended production procedure:

1. Add `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`, and `BOOTSTRAP_ADMIN_NAME` to Render.
2. Deploy.
3. Verify the first ADMIN exists.
4. Remove `BOOTSTRAP_ADMIN_PASSWORD` from Render.
5. Redeploy or restart normally; bootstrap is idempotent because an ADMIN already exists.

## Environment Variables

Required database/runtime values:

```text
SPRING_PROFILES_ACTIVE
PORT
DB_URL
DB_USERNAME
DB_PASSWORD
DB_SCHEMA
CORS_ALLOWED_ORIGINS
JWT_SECRET_BASE64
```

Optional Phase 8 operational values:

```text
SWAGGER_ENABLED=true
MAX_JSON_BODY_BYTES=1048576
CLOUDINARY_HTTP_TIMEOUT_SECONDS=10
```

Phase 2 auth variables:

```text
JWT_ISSUER=dongbac-backend
JWT_ACCESS_TOKEN_MINUTES=15
REFRESH_TOKEN_DAYS=7
AUTH_REFRESH_COOKIE_NAME=dbsg_refresh
AUTH_REFRESH_COOKIE_SECURE=false
AUTH_REFRESH_COOKIE_SAME_SITE=Lax
AUTH_MAX_FAILED_ATTEMPTS=5
AUTH_LOCK_MINUTES=15
BOOTSTRAP_ADMIN_EMAIL=
BOOTSTRAP_ADMIN_PASSWORD=
BOOTSTRAP_ADMIN_NAME=
```

Phase 4 Cloudinary/media variables:

```text
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
CLOUDINARY_ROOT_FOLDER=dongbac
CLOUDINARY_HTTP_TIMEOUT_SECONDS=10
MEDIA_IMAGE_MAX_BYTES=10485760
MEDIA_VIDEO_MAX_BYTES=209715200
MEDIA_UPLOAD_INTENT_MINUTES=15
```

Generate a JWT secret in PowerShell:

```powershell
$bytes = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Use the output as `JWT_SECRET_BASE64`. Never commit the generated value.

## Local Windows Setup

Set environment variables in PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:PORT="8080"
$env:SWAGGER_ENABLED="true"
$env:MAX_JSON_BODY_BYTES="1048576"
$env:DB_URL="jdbc:postgresql://<SUPABASE_SESSION_POOLER_HOST>:5432/postgres?sslmode=require"
$env:DB_USERNAME="postgres.<PROJECT_REF>"
$env:DB_PASSWORD="<SECRET>"
$env:DB_SCHEMA="dongbac"
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173,http://localhost:5174"
$env:JWT_SECRET_BASE64="<GENERATED_SECRET>"
$env:JWT_ISSUER="dongbac-backend"
$env:JWT_ACCESS_TOKEN_MINUTES="15"
$env:REFRESH_TOKEN_DAYS="7"
$env:AUTH_REFRESH_COOKIE_NAME="dbsg_refresh"
$env:AUTH_REFRESH_COOKIE_SECURE="false"
$env:AUTH_REFRESH_COOKIE_SAME_SITE="Lax"
$env:AUTH_MAX_FAILED_ATTEMPTS="5"
$env:AUTH_LOCK_MINUTES="15"
$env:BOOTSTRAP_ADMIN_EMAIL="<ADMIN_EMAIL>"
$env:BOOTSTRAP_ADMIN_PASSWORD="<STRONG_PASSWORD>"
$env:BOOTSTRAP_ADMIN_NAME="<ADMIN_NAME>"
$env:CLOUDINARY_CLOUD_NAME="<CLOUDINARY_CLOUD_NAME>"
$env:CLOUDINARY_API_KEY="<CLOUDINARY_API_KEY>"
$env:CLOUDINARY_API_SECRET="<CLOUDINARY_API_SECRET>"
$env:CLOUDINARY_ROOT_FOLDER="dongbac"
$env:CLOUDINARY_HTTP_TIMEOUT_SECONDS="10"
$env:MEDIA_IMAGE_MAX_BYTES="10485760"
$env:MEDIA_VIDEO_MAX_BYTES="209715200"
$env:MEDIA_UPLOAD_INTENT_MINUTES="15"
```

Build and test:

```powershell
.\mvnw.cmd clean package
```

Run locally:

```powershell
.\mvnw.cmd spring-boot:run
```

Useful URLs:

```text
http://localhost:8080/actuator/health
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
http://localhost:8080/api/public/ping
```

Swagger can authorize with a Bearer access token. Refresh and logout require the backend-origin refresh cookie plus `X-CSRF-Guard: 1`. For Phase 7, use an ADMIN token for Lead assignment and global pending Approval counts. Use either role for Lead CRUD-style operations, Lead status updates, and Dashboard summary access.

### Manual Phase 6 Article Swagger Flow

1. Login as ADMIN and authorize Swagger with the access token.
2. Upload an image using the existing Media signed-upload flow and complete the upload intent.
3. Create a `NEWS`, `KNOWLEDGE`, or `INTERNAL_ACTIVITY` Article DRAFT that references the returned `mediaId`.
4. Include unsafe HTML such as a `script` element or event handler, then confirm the stored DRAFT contains only sanitized allowed HTML.
5. Confirm the Article is absent from public APIs.
6. Submit Revision 1 and confirm an `ARTICLE` request appears in Approval Center with Article ID plus revision number.
7. Approve the request and confirm the Article is public with a safe featured-image DTO.
8. Create the next draft revision, edit it, submit it, then reject it.
9. Confirm the previous published revision remains public and unchanged.
10. Create another revision, submit and approve it, then confirm public detail switches atomically.
11. Test unpublish, republish of the unchanged approved revision, archive, and the `409 Conflict` returned when deleting referenced Media.

### Manual Phase 7 Lead And Dashboard Swagger Flow

1. Login as ADMIN and authorize Swagger with the access token.
2. Create a Lead with a manually supplied real test name plus at least a phone or email; confirm status is `NEW`.
3. Read Lead detail, list Leads, search it, and filter `status=NEW`.
4. Change status to `CONTACTED`; confirm the same Lead is updated.
5. Assign it to an active STAFF account; confirm safe assignee summary fields only.
6. Call `GET /api/admin/dashboard/summary`; verify Lead totals/status count, Product identity counts, Article identity counts, active Media, and global pending Approvals.
7. Login as STAFF; verify list/read/update/status operations work and assignment returns `403`.
8. Call Dashboard as STAFF and confirm pending Approvals count only that STAFF user's submissions.
9. Submit `POST /api/public/contact` without a token; confirm it creates a `NEW` Lead visible to Admin and increases the Dashboard Lead count. There is no `/api/public/leads` read endpoint.

## Render Configuration

Existing production values:

```text
SPRING_PROFILES_ACTIVE=prod
SWAGGER_ENABLED=false
MAX_JSON_BODY_BYTES=1048576
DB_URL=<Supabase Session Pooler JDBC URL with sslmode=require>
DB_USERNAME=postgres.<PROJECT_REF>
DB_PASSWORD=<SECRET>
DB_SCHEMA=dongbac
CORS_ALLOWED_ORIGINS=https://dong-bac-group.vercel.app,https://admin-dongbac-lhzv.vercel.app
```

Add Phase 2 values:

```text
JWT_SECRET_BASE64=<SECRET>
JWT_ISSUER=dongbac-backend
JWT_ACCESS_TOKEN_MINUTES=15
REFRESH_TOKEN_DAYS=7
AUTH_REFRESH_COOKIE_NAME=dbsg_refresh
AUTH_REFRESH_COOKIE_SECURE=true
AUTH_REFRESH_COOKIE_SAME_SITE=None
AUTH_MAX_FAILED_ATTEMPTS=5
AUTH_LOCK_MINUTES=15
BOOTSTRAP_ADMIN_EMAIL=<FIRST_ADMIN_EMAIL>
BOOTSTRAP_ADMIN_PASSWORD=<TEMPORARY_RENDER_SECRET>
BOOTSTRAP_ADMIN_NAME=<NAME>
```

Add Phase 4 values:

```text
CLOUDINARY_CLOUD_NAME=<SECRET>
CLOUDINARY_API_KEY=<SECRET>
CLOUDINARY_API_SECRET=<SECRET>
CLOUDINARY_ROOT_FOLDER=dongbac
CLOUDINARY_HTTP_TIMEOUT_SECONDS=10
MEDIA_IMAGE_MAX_BYTES=10485760
MEDIA_VIDEO_MAX_BYTES=209715200
MEDIA_UPLOAD_INTENT_MINUTES=15
```

Render health check remains:

```text
/actuator/health
```

Phase 8 itself added no secret or migration. This later frontend-compatibility patch adds V8. `SWAGGER_ENABLED` and `CLOUDINARY_HTTP_TIMEOUT_SECONDS` are optional operational settings with safe profile defaults. Do not manually create the application tables in Supabase. Flyway V2 through V8 apply automatically on deployment; a successful startup should show Flyway at schema version 8 followed by Hibernate schema validation.

## Verification SQL

Tables:

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'dongbac'
ORDER BY table_name;
```

Expected Phase 7 application tables:

```text
approval_requests
article_revision_media
article_revisions
articles
audit_logs
categories
customer_leads
flyway_schema_history
manufacturing_page
manufacturing_sections
media
media_upload_intents
product_revision_images
product_revision_related_products
product_revisions
products
refresh_tokens
site_banners
site_partners
site_settings
users
```

Flyway history:

```sql
SELECT installed_rank,
       version,
       description,
       success
FROM dongbac.flyway_schema_history
ORDER BY installed_rank;
```

Expected:

- V1 success
- V2 success
- V3 success
- V4 success
- V5 success
- V6 success
- V7 success
- V8 success

## Security Notes

- Access tokens are JWTs signed with HMAC SHA-256.
- Refresh tokens are opaque, random, high-entropy, and stored only as SHA-256 hashes.
- Refresh tokens rotate on use.
- Refresh cookies are HttpOnly.
- Production refresh cookies are Secure and SameSite=None.
- CORS allows configured origins only and uses credentials; wildcard origins are rejected.
- Passwords are BCrypt hashes.
- API responses never return password hashes or raw refresh tokens.
- API responses never return `CLOUDINARY_API_SECRET`.
- Normal media uploads go browser-to-Cloudinary directly, not through Render.
- Media database rows are created only after the backend verifies the Cloudinary resource server-side.
- Referenced media cannot be deleted.
- Product images are references to active Phase 4 Media; Product APIs never receive file bytes or Cloudinary credentials.
- Submitted Product revisions are immutable and public APIs resolve only the current published revision.
- Submitted Article revisions are immutable, Article HTML is sanitized server-side, and public APIs resolve only the current published revision.
- Article media is limited to active images and all Article revision references participate in media deletion protection.
- STAFF cannot mutate Categories or invoke Product publication lifecycle commands.
- STAFF cannot approve, hard-delete, unpublish, republish, or archive Articles.
- Lead read/manage APIs remain authenticated under `/api/admin/leads`; public contact intake returns no customer Lead data.
- `CustomerLead` uses optimistic locking, has no hard-delete API, and assignment is `ADMIN` only.
- Lead audit metadata excludes customer phone, email, message, and internal note content.
- Dashboard pending Approval counts preserve the global `ADMIN` and owner-scoped `STAFF` boundary.
- Security-sensitive events are logged without passwords, token values, cookies, JWTs, or secrets.
- Audit logs are persistent and append-only.
- Approval decisions are concurrency-safe with database row locking and optimistic versioning.
- `X-Correlation-ID` is returned on responses and stored with audit records when available.
- Unknown routes are denied by default; public, authentication, actuator, Swagger, and admin routes are explicitly declared.
- Production exposes only Actuator health, hides health details, disables Swagger by default, and uses graceful shutdown behind forwarded proxy headers.
- Cloudinary management calls use a bounded HTTP timeout. The Cloudinary API secret remains backend-only.
- JSON, form, and multipart request sizes are bounded; normal media bytes still upload directly to Cloudinary.
- Unexpected API responses are generic; stack traces, SQL details, filesystem paths, and exception messages are not returned to clients.

## Production Operations

### Security Model

- Access tokens are short-lived HS256 JWTs sent as Bearer tokens. Signature, expiration, and issuer are validated server-side; role claims are accepted only from a valid signed token.
- Refresh tokens are opaque random values. Only their SHA-256 hashes are stored, and each successful refresh rotates and revokes the previous token.
- Refresh and logout use the HttpOnly refresh cookie and require `X-CSRF-Guard: 1`. Business APIs use the Bearer token and do not rely on the refresh cookie.
- Local cookie values are `Secure=false` and `SameSite=Lax`. Production values are `Secure=true` and `SameSite=None` for the cross-site Vercel Admin frontend and Render backend.
- Credentialed CORS accepts only exact origins from `CORS_ALLOWED_ORIGINS`; wildcard origins and origin URLs with paths are rejected at startup.

### Production Checklist

Required secrets are `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_BASE64`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, and `CLOUDINARY_API_SECRET`. Set `SPRING_PROFILES_ACTIVE=prod`, retain the non-secret values in `render.yaml`, and set `CORS_ALLOWED_ORIGINS` to the two approved Vercel origins. `DB_URL` must continue to target the Supabase PostgreSQL Session Pooler with TLS.

Bootstrap variables are required only for initial ADMIN creation. Bootstrap creates an ADMIN only when none exists; it never resets an existing password. Remove `BOOTSTRAP_ADMIN_PASSWORD` after confirming the initial ADMIN and restart normally.

Flyway is the only schema owner and Hibernate remains `ddl-auto=validate`. Migrations are forward-only: never edit V1-V7, delete Flyway history rows, or run Flyway clean in production. V8 belongs to this later frontend-compatibility patch.

Render uses `PORT`, `/actuator/health`, Java 17, a non-root container user, graceful shutdown, and the production profile. Swagger is disabled in production unless `SWAGGER_ENABLED=true` is an explicit operational decision. Liveness and readiness are available below health without exposing component details.

### Deployment Smoke Test

After deployment, perform read-only or low-risk checks:

1. Confirm `GET /actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness` return healthy responses.
2. Read public Categories, Products, Articles, Site content, and Manufacturing content; verify only active/published data appears.
3. Login with an authorized ADMIN test account, call `GET /api/admin/auth/me`, then read Dashboard, Media, Approval, and Lead lists.
4. Verify refresh rotates the HttpOnly cookie and logout invalidates it. Browser cross-site acceptance from the Admin frontend is deferred to frontend integration.
5. For an explicitly approved Cloudinary smoke test, upload one known test image through signature/direct-upload/complete and delete it only when it is unreferenced.
6. Query `dongbac.flyway_schema_history` and confirm V1-V8 succeeded. Check `information_schema.tables` for the documented application tables, including `manufacturing_services`.

Do not create, publish, reject, archive, or delete real production business content merely for a smoke test.

### Rollback And Recovery

Roll back application code by deploying a previous known-good image or commit. Database migrations are forward-only; correct a schema issue with a reviewed follow-up migration. Never remove migration-history rows or edit an applied migration. Supabase backup and restore policy is an infrastructure responsibility and must be verified in the Supabase project; this repository does not claim or implement a backup schedule.

### Troubleshooting

- Startup fails before Tomcat: verify required environment values are nonblank, `JWT_SECRET_BASE64` decodes to at least 32 bytes, CORS origins are exact HTTP(S) origins, and Cloudinary settings are present.
- Flyway or JPA validation fails: compare `dongbac.flyway_schema_history`, migration checksums, schema name, and deployed application version. Do not bypass validation with `ddl-auto=update`.
- Cross-site refresh fails: verify the browser request uses credentials, production cookie values are `Secure=true` and `SameSite=None`, the exact Admin origin is allowed, and `X-CSRF-Guard: 1` is present.
- Health is down: inspect Render logs using the response correlation ID, then check Supabase connectivity and application startup. Production logs intentionally omit request bodies, token values, secrets, and internal exception messages.
- Cloudinary verification times out: check Cloudinary status/connectivity and adjust `CLOUDINARY_HTTP_TIMEOUT_SECONDS` conservatively; do not disable the timeout.

## Deferred Beyond Phase 8

Not implemented in this phase:

- Frontend API integration
- Public login or registration
- Multiple-admin management APIs
- Recruitment CMS
- Public Lead/contact intake
- Customer accounts, Orders, Invoices, and Payments
- CRM, email, SMS, Zalo, OneBSS, marketing automation, and notifications
- Revenue, financial, traffic, visitor, page-view, and conversion analytics
- Redis/JWT blacklist
- Product-specific fertilizer attributes not defined by Phase 5
- Draft preview tokens
- Article categories, tags, comments, likes, view counters, newsletter, and publication scheduling
- Search engine service, Google Docs synchronization, and AI content generation

Root `/` intentionally has no business route and is covered by the deny-by-default security fallback.
